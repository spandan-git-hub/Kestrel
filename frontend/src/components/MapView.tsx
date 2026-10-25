import React, { useEffect, useMemo, useState } from 'react';
import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  useMap,
  ZoomControl,
} from 'react-leaflet';
import L from 'leaflet';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setSelectedLocation, setViewport } from '../features/map/mapSlice';
import { useGetLocationsQuery } from '../api/registryApi';
import { useGetAlertStatusesQuery } from '../api/rulesApi';
import { SeverityBadge } from './SeverityBadge';
import { ProvenanceBadge } from './ProvenanceBadge';
import type { LocationDto, Severity, Provenance } from '../types/api';
import {
  Maximize2,
  Navigation,
  Layers,
  MapPin,
  Info,
} from 'lucide-react';

export interface MapViewProps {
  className?: string;
  height?: string | number;
  onSelectLocation?: (locationId: string) => void;
  showQuickJump?: boolean;
  showLegend?: boolean;
}

// Canonical seeded 4 pilot locations in India matching backend registry
export const SEEDED_PILOT_LOCATIONS: LocationDto[] = [
  {
    id: 'mumbai-kurla',
    name: 'Kurla Ward L, Mumbai',
    state: 'Maharashtra',
    latitude: 19.0726,
    longitude: 72.8797,
    riverBasin: 'Mithi River',
    threshold2yr: 45.0,
    threshold5yr: 85.0,
    threshold20yr: 140.0,
    primaryLanguage: 'mr',
    active: true,
  },
  {
    id: 'patna-ward12',
    name: 'Ward 12, Patna',
    state: 'Bihar',
    latitude: 25.6127,
    longitude: 85.1589,
    riverBasin: 'Ganges River',
    threshold2yr: 38000.0,
    threshold5yr: 48000.0,
    threshold20yr: 62000.0,
    primaryLanguage: 'hi',
    active: true,
  },
  {
    id: 'guwahati-river',
    name: 'Riverside, Guwahati',
    state: 'Assam',
    latitude: 26.1856,
    longitude: 91.7478,
    riverBasin: 'Brahmaputra River',
    threshold2yr: 32000.0,
    threshold5yr: 42000.0,
    threshold20yr: 55000.0,
    primaryLanguage: 'en',
    active: true,
  },
  {
    id: 'cuttack-delta',
    name: 'Cuttack Municipal Corp',
    state: 'Odisha',
    latitude: 20.4625,
    longitude: 85.8830,
    riverBasin: 'Mahanadi River',
    threshold2yr: 18000.0,
    threshold5yr: 26000.0,
    threshold20yr: 35000.0,
    primaryLanguage: 'en',
    active: true,
  },
];

const INDIA_CENTER: [number, number] = [21.5, 82.0];
const INDIA_ZOOM = 5;

// SVG icons embedded as string templates for Leaflet DivIcon
const ICONS_SVG = {
  SHIELD:
    '<svg class="w-3.5 h-3.5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>',
  WATCH:
    '<svg class="w-3.5 h-3.5 text-slate-900" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>',
  WARNING:
    '<svg class="w-4 h-4 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
  SEVERE:
    '<svg class="w-4 h-4 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/></svg>',
};

/**
 * Creates custom pulsing Leaflet divIcon depending on severity level.
 * Pulsing ripple (animate-ping) is active on WARNING and SEVERE.
 */
function createHazardDivIcon(
  severity: Severity,
  isSelected: boolean,
  shortName: string
): L.DivIcon {
  let markerHtml = '';
  const selectedRingClass = isSelected
    ? 'ring-4 ring-blue-400 ring-offset-2 ring-offset-slate-900 scale-110 shadow-blue-500/50'
    : '';

  switch (severity) {
    case 'SEVERE':
      markerHtml = `
        <div class="relative flex flex-col items-center group cursor-pointer">
          <span class="absolute top-0 -inset-3 w-12 h-12 rounded-full bg-red-600/60 animate-ping"></span>
          <span class="absolute top-0.5 -inset-1.5 w-9 h-9 rounded-full bg-red-500/40 animate-pulse"></span>
          <div class="relative z-10 w-9 h-9 rounded-full bg-gradient-to-br from-red-500 to-red-700 border-2 border-red-100 shadow-2xl shadow-red-950 flex items-center justify-center text-white transition-transform ${selectedRingClass}">
            ${ICONS_SVG.SEVERE}
          </div>
          <div class="mt-1 px-2 py-0.5 rounded text-[10px] font-bold tracking-tight whitespace-nowrap bg-slate-950/90 text-red-300 border border-red-500/40 shadow-lg pointer-events-none">
            ${shortName}
          </div>
        </div>
      `;
      break;

    case 'WARNING':
      markerHtml = `
        <div class="relative flex flex-col items-center group cursor-pointer">
          <span class="absolute top-0.5 -inset-2 w-10 h-10 rounded-full bg-orange-500/50 animate-ping"></span>
          <div class="relative z-10 w-8 h-8 rounded-full bg-gradient-to-br from-orange-400 to-orange-600 border-2 border-orange-100 shadow-xl shadow-orange-950 flex items-center justify-center text-white transition-transform ${selectedRingClass}">
            ${ICONS_SVG.WARNING}
          </div>
          <div class="mt-1 px-2 py-0.5 rounded text-[10px] font-semibold tracking-tight whitespace-nowrap bg-slate-950/90 text-orange-300 border border-orange-500/40 shadow-lg pointer-events-none">
            ${shortName}
          </div>
        </div>
      `;
      break;

    case 'WATCH':
      markerHtml = `
        <div class="relative flex flex-col items-center group cursor-pointer">
          <div class="relative z-10 w-8 h-8 rounded-full bg-gradient-to-br from-amber-300 to-amber-500 border-2 border-amber-100 shadow-lg shadow-amber-950 flex items-center justify-center text-slate-950 transition-transform ${selectedRingClass}">
            ${ICONS_SVG.WATCH}
          </div>
          <div class="mt-1 px-2 py-0.5 rounded text-[10px] font-semibold tracking-tight whitespace-nowrap bg-slate-950/90 text-amber-300 border border-amber-500/40 shadow-lg pointer-events-none">
            ${shortName}
          </div>
        </div>
      `;
      break;

    case 'NONE':
    default:
      markerHtml = `
        <div class="relative flex flex-col items-center group cursor-pointer">
          <div class="relative z-10 w-7 h-7 rounded-full bg-gradient-to-br from-emerald-400 to-emerald-600 border-2 border-emerald-200 shadow-md shadow-emerald-950 flex items-center justify-center text-white transition-transform ${selectedRingClass}">
            ${ICONS_SVG.SHIELD}
          </div>
          <div class="mt-1 px-2 py-0.5 rounded text-[10px] font-medium tracking-tight whitespace-nowrap bg-slate-950/90 text-slate-300 border border-slate-700/80 shadow pointer-events-none">
            ${shortName}
          </div>
        </div>
      `;
      break;
  }

  return L.divIcon({
    className: 'custom-hazard-marker',
    html: markerHtml,
    iconSize: [40, 52],
    iconAnchor: [20, 20],
    popupAnchor: [0, -22],
  });
}

/**
 * Helper component that manages smooth pan/flyTo when selected location changes in Redux.
 */
interface MapControllerProps {
  selectedLocation: LocationDto | null;
  shouldPan: boolean;
}

const MapController: React.FC<MapControllerProps> = ({ selectedLocation, shouldPan }) => {
  const map = useMap();

  useEffect(() => {
    if (shouldPan && selectedLocation && map) {
      try {
        map.flyTo([selectedLocation.latitude, selectedLocation.longitude], 7, {
          duration: 1.2,
        });
      } catch {
        // Safe fallback in case map container was detached during animation
      }
    }
    return () => {
      try {
        map?.stop();
      } catch {
        // ignore
      }
    };
  }, [selectedLocation?.id, shouldPan, map]);

  return null;
};

export const MapView: React.FC<MapViewProps> = ({
  className = '',
  height = '480px',
  onSelectLocation,
  showQuickJump = true,
  showLegend = true,
}) => {
  const dispatch = useAppDispatch();
  const selectedLocationId = useAppSelector((state) => state.map.selectedLocationId);
  const activeLanguage = useAppSelector((state) => state.ui.language);

  const [tileMode, setTileMode] = useState<'dark' | 'standard' | 'satellite'>('standard');
  const [shouldAutoPan, setShouldAutoPan] = useState(false);

  // RTK Query hooks
  const { data: registryLocations } = useGetLocationsQuery();
  const { data: alertStatuses } = useGetAlertStatusesQuery();

  // Merge registry locations or use seeded fallback
  const locations: LocationDto[] = useMemo(() => {
    if (registryLocations && registryLocations.length > 0) {
      return registryLocations;
    }
    return SEEDED_PILOT_LOCATIONS;
  }, [registryLocations]);

  // Lookup severity and status by location id
  const statusByLocationId = useMemo(() => {
    const map = new Map<string, { severity: Severity; provenance: Provenance; peak?: number; date?: string }>();
    alertStatuses?.forEach((status) => {
      map.set(status.locationId, {
        severity: status.severity,
        provenance: status.provenance,
        peak: status.peakExceedanceShare ?? undefined,
        date: status.firstExceedanceDate ?? undefined,
      });
    });
    return map;
  }, [alertStatuses]);

  // Current selected location object
  const activeLocation = useMemo(() => {
    return locations.find((l) => l.id === selectedLocationId) || locations[0] || null;
  }, [locations, selectedLocationId]);

  // Summary counts
  const stats = useMemo(() => {
    let severeCount = 0;
    let warningCount = 0;
    let watchCount = 0;
    let safeCount = 0;

    locations.forEach((loc) => {
      const s = statusByLocationId.get(loc.id)?.severity || 'NONE';
      if (s === 'SEVERE') severeCount++;
      else if (s === 'WARNING') warningCount++;
      else if (s === 'WATCH') watchCount++;
      else safeCount++;
    });

    return { total: locations.length, severeCount, warningCount, watchCount, safeCount };
  }, [locations, statusByLocationId]);

  const handleMarkerClick = (locationId: string, lat: number, lon: number) => {
    dispatch(setSelectedLocation(locationId));
    dispatch(setViewport({ center: [lat, lon], zoom: 7 }));
    if (onSelectLocation) {
      onSelectLocation(locationId);
    }
  };

  const handleResetView = () => {
    setShouldAutoPan(false);
    dispatch(setViewport({ center: INDIA_CENTER, zoom: INDIA_ZOOM }));
  };

  return (
    <div
      className={`relative isolate w-full rounded-2xl overflow-hidden border border-slate-800 shadow-2xl bg-[#0b0f19] flex flex-col ${className}`}
      style={{ height: typeof height === 'number' ? `${height}px` : height }}
    >
      {/* Top Map HUD Bar - elevated with z-[1001] to always float above Leaflet controls */}
      <div className="absolute top-3 left-3 right-3 z-[1001] flex flex-wrap items-center justify-between gap-2 pointer-events-none">
        {/* Left Status Pill */}
        <div className="pointer-events-auto flex items-center space-x-2 bg-slate-900/95 backdrop-blur-md border border-slate-700/90 px-3 py-1.5 rounded-xl shadow-xl">
          <div className="p-1 rounded-md bg-blue-500/20 text-blue-400">
            <Navigation className="w-3.5 h-3.5" />
          </div>
          <div className="text-xs">
            <span className="font-semibold text-slate-100">India Hazard Map</span>
            <span className="mx-1.5 text-slate-600">·</span>
            <span className="text-slate-400">
              {stats.total} Pilot Locations (
              {stats.severeCount > 0 ? (
                <span className="text-red-400 font-semibold">{stats.severeCount} Severe</span>
              ) : stats.warningCount > 0 ? (
                <span className="text-orange-400 font-semibold">{stats.warningCount} Warning</span>
              ) : (
                <span className="text-emerald-400 font-semibold">All Safe</span>
              )}
              )
            </span>
          </div>
        </div>

        {/* Right Map Style & Action Buttons */}
        <div className="pointer-events-auto flex items-center space-x-1 bg-slate-900/95 backdrop-blur-md border border-slate-700/90 p-1.5 rounded-xl shadow-xl text-xs">
          <div className="flex items-center space-x-1 mr-1">
            <Layers className="w-3.5 h-3.5 text-blue-400 ml-1" />
            <span className="text-[10px] uppercase font-bold text-slate-400 hidden sm:inline">
              Style:
            </span>
          </div>

          <button
            onClick={() => setTileMode('dark')}
            title="Dark GIS Base Map"
            className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-all ${
              tileMode === 'dark'
                ? 'bg-blue-600 text-white shadow-md'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
            }`}
          >
            Dark GIS
          </button>

          <button
            onClick={() => setTileMode('standard')}
            title="OpenStreetMap Street View"
            className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-all ${
              tileMode === 'standard'
                ? 'bg-blue-600 text-white shadow-md'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
            }`}
          >
            Streets
          </button>

          <button
            onClick={() => setTileMode('satellite')}
            title="ESRI Satellite Imagery"
            className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-all ${
              tileMode === 'satellite'
                ? 'bg-blue-600 text-white shadow-md'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
            }`}
          >
            Satellite
          </button>

          <div className="w-[1px] h-4 bg-slate-700 mx-1" />

          {/* Reset India View Button */}
          <button
            onClick={() => handleResetView()}
            title="Reset to India Overview"
            className="flex items-center space-x-1 px-2 py-1 rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <Maximize2 className="w-3.5 h-3.5 text-emerald-400" />
            <span className="text-[11px] font-semibold hidden sm:inline">India</span>
          </button>
        </div>
      </div>

      {/* Leaflet Map Container */}
      <div className="relative flex-1 w-full h-full">
        <MapContainer
          center={INDIA_CENTER}
          zoom={INDIA_ZOOM}
          minZoom={4}
          maxZoom={14}
          scrollWheelZoom={true}
          zoomControl={false}
          className="w-full h-full"
        >
          {/* Tile Layers based on tileMode */}
          {tileMode === 'dark' ? (
            <TileLayer
              key="dark"
              attribution='Tiles &copy; Esri &mdash; Esri, DeLorme, NAVTEQ, OpenStreetMap'
              url="https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}"
              maxZoom={16}
            />
          ) : tileMode === 'satellite' ? (
            <TileLayer
              key="satellite"
              attribution='Tiles &copy; Esri &mdash; Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community'
              url="https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
              maxZoom={18}
            />
          ) : (
            <TileLayer
              key="standard"
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              maxZoom={19}
            />
          )}

          {/* Controls: Placed bottom-right so it never collides with top HUD bar */}
          <ZoomControl position="bottomright" />
          <MapController selectedLocation={activeLocation} shouldPan={shouldAutoPan} />

          {/* Markers for all 4 Pilot Locations */}
          {locations.map((loc) => {
            const locStatus = statusByLocationId.get(loc.id);
            const severity: Severity = locStatus?.severity || 'NONE';
            const provenance: Provenance = locStatus?.provenance || 'MODEL_ONLY';
            const isSelected = loc.id === selectedLocationId;
            const shortName = loc.name.split(',')[0].replace(' Ward L', '').trim();

            const customIcon = createHazardDivIcon(severity, isSelected, shortName);

            return (
              <Marker
                key={loc.id}
                position={[loc.latitude, loc.longitude]}
                icon={customIcon}
                eventHandlers={{
                  click: () => {
                    setShouldAutoPan(true);
                    handleMarkerClick(loc.id, loc.latitude, loc.longitude);
                  },
                }}
              >
                <Popup className="kestrel-map-popup">
                  <div className="p-4 w-72 space-y-3 font-sans text-slate-100">
                    {/* Header */}
                    <div className="border-b border-slate-800 pb-2">
                      <div className="flex items-center justify-between text-[11px] text-blue-400 font-mono">
                        <span>{loc.riverBasin || 'River Basin'}</span>
                        <span className="text-slate-400">{loc.state}</span>
                      </div>
                      <h3 className="text-sm font-bold text-slate-100 leading-snug mt-0.5">
                        {loc.name}
                      </h3>
                      <div className="text-[11px] text-slate-400 font-mono mt-0.5">
                        {loc.latitude.toFixed(4)}° N, {loc.longitude.toFixed(4)}° E
                      </div>
                    </div>

                    {/* Severity and Provenance Badges */}
                    <div className="space-y-1.5">
                      <div className="flex items-center justify-between">
                        <span className="text-[11px] text-slate-400">Hazard State:</span>
                        <SeverityBadge severity={severity} size="sm" language={activeLanguage} />
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-[11px] text-slate-400">Data Origin:</span>
                        <ProvenanceBadge provenance={provenance} size="sm" language={activeLanguage} />
                      </div>
                    </div>

                    {/* Hydrological Return Period Benchmarks */}
                    <div className="bg-slate-900/90 rounded-lg p-2.5 border border-slate-800 space-y-1.5 text-[11px]">
                      <div className="text-[10px] uppercase tracking-wider text-slate-400 font-semibold flex items-center space-x-1">
                        <Info className="w-3 h-3 text-blue-400" />
                        <span>Return Period Benchmarks</span>
                      </div>
                      <div className="grid grid-cols-3 gap-1.5 text-center pt-1 font-mono">
                        <div className="bg-slate-800/80 rounded p-1">
                          <div className="text-[9px] text-amber-400 font-semibold">2-Yr</div>
                          <div className="text-slate-200">{loc.threshold2yr} m³/s</div>
                        </div>
                        <div className="bg-slate-800/80 rounded p-1">
                          <div className="text-[9px] text-orange-400 font-semibold">5-Yr</div>
                          <div className="text-slate-200">{loc.threshold5yr} m³/s</div>
                        </div>
                        <div className="bg-slate-800/80 rounded p-1">
                          <div className="text-[9px] text-red-400 font-semibold">20-Yr</div>
                          <div className="text-slate-200">{loc.threshold20yr} m³/s</div>
                        </div>
                      </div>
                    </div>

                    {/* Exceedance Stats if Active */}
                    {locStatus?.peak !== undefined && locStatus.peak > 0 && (
                      <div className="p-2 rounded-lg bg-orange-950/40 border border-orange-800/40 text-[11px] text-orange-200 flex items-center justify-between">
                        <span>Ensemble Breach Share:</span>
                        <span className="font-bold font-mono">
                          {Math.round(locStatus.peak * 100)}% of members
                        </span>
                      </div>
                    )}

                    {/* Select Action Button */}
                    <button
                      onClick={() => {
                        dispatch(setSelectedLocation(loc.id));
                        if (onSelectLocation) onSelectLocation(loc.id);
                      }}
                      className={`w-full py-2 px-3 rounded-lg text-xs font-semibold flex items-center justify-center space-x-1.5 transition-colors shadow-sm ${
                        isSelected
                          ? 'bg-blue-600/30 text-blue-300 border border-blue-500/50'
                          : 'bg-blue-600 hover:bg-blue-500 text-white'
                      }`}
                    >
                      <MapPin className="w-3.5 h-3.5" />
                      <span>{isSelected ? 'Currently Selected' : 'Select for Forecast Analysis'}</span>
                    </button>
                  </div>
                </Popup>
              </Marker>
            );
          })}
        </MapContainer>
      </div>

      {/* Bottom Map Legend & Quick Jump Bar */}
      <div className="absolute bottom-3 left-3 right-3 z-[400] flex flex-col sm:flex-row items-start sm:items-end justify-between gap-2 pointer-events-none">
        {/* Severity Legend */}
        {showLegend && (
          <div className="pointer-events-auto bg-slate-950/90 backdrop-blur-md border border-slate-800/90 px-3 py-2 rounded-xl shadow-xl flex items-center space-x-3 text-[11px] text-slate-300">
            <span className="font-semibold text-slate-400 uppercase tracking-wider text-[10px]">
              Risk Legend:
            </span>
            <div className="flex items-center space-x-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 shadow-sm shadow-emerald-500/50" />
              <span>Safe</span>
            </div>
            <div className="flex items-center space-x-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-amber-500 shadow-sm shadow-amber-500/50" />
              <span>Watch (2-Yr)</span>
            </div>
            <div className="flex items-center space-x-1.5">
              <span className="relative flex h-2.5 w-2.5">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-orange-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-orange-500"></span>
              </span>
              <span>Warning (5-Yr)</span>
            </div>
            <div className="flex items-center space-x-1.5">
              <span className="relative flex h-2.5 w-2.5">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-red-600"></span>
              </span>
              <span className="text-red-300 font-semibold">Severe (20-Yr)</span>
            </div>
          </div>
        )}

        {/* Quick Jump Pills for 4 Pilot Locations */}
        {showQuickJump && (
          <div className="pointer-events-auto flex items-center space-x-1.5 bg-slate-950/90 backdrop-blur-md border border-slate-800/90 p-1.5 rounded-xl shadow-xl">
            <span className="text-[10px] text-slate-400 font-semibold px-1.5 uppercase hidden md:inline">
              Quick Focus:
            </span>
            {locations.map((loc) => {
              const s = statusByLocationId.get(loc.id)?.severity || 'NONE';
              const isSel = loc.id === selectedLocationId;
              const short = loc.name.split(',')[0].replace('Ward L', '').trim();

              const dotColor =
                s === 'SEVERE'
                  ? 'bg-red-500'
                  : s === 'WARNING'
                  ? 'bg-orange-500'
                  : s === 'WATCH'
                  ? 'bg-amber-400'
                  : 'bg-emerald-400';

              return (
                <button
                  key={loc.id}
                  onClick={() => {
                    setShouldAutoPan(true);
                    handleMarkerClick(loc.id, loc.latitude, loc.longitude);
                  }}
                  className={`flex items-center space-x-1.5 px-2.5 py-1 rounded-lg text-xs font-medium transition-colors ${
                    isSel
                      ? 'bg-blue-600 text-white shadow-sm'
                      : 'bg-slate-800/80 text-slate-300 hover:text-white hover:bg-slate-700/80'
                  }`}
                >
                  <span className={`w-2 h-2 rounded-full ${dotColor}`} />
                  <span>{short}</span>
                </button>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default MapView;
