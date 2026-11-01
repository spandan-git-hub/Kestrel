import React, { useEffect, useMemo } from 'react';
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
import { SeverityBadge } from './SeverityBadge';
import { ProvenanceBadge } from './ProvenanceBadge';
import type { LocationDto, Severity, Provenance } from '../types/api';
import { MapPin } from 'lucide-react';

export const BASIN_OPTIONS = [
  { id: 'ALL', label: 'All Basins (45 Stations)' },
  { id: 'Ganga Basin', label: 'Ganga Basin (14 Stations)' },
  { id: 'Brahmaputra Basin', label: 'Brahmaputra Basin (6 Stations)' },
  { id: 'Indus/North', label: 'Indus & North Himalayan (5 Stations)' },
  { id: 'Godavari Basin', label: 'Godavari Basin (4 Stations)' },
  { id: 'Krishna Basin', label: 'Krishna Basin (4 Stations)' },
  { id: 'Mahanadi Basin', label: 'Mahanadi Basin (2 Stations)' },
  { id: 'West-Flowing', label: 'West-Flowing (4 Stations)' },
  { id: 'Southern', label: 'Southern Basins (4 Stations)' },
  { id: 'Eastern', label: 'Eastern Basin (1 Station)' },
  { id: 'West Coastal', label: 'West Coastal (1 Station)' },
];

export interface MapViewProps {
  className?: string;
  height?: string | number;
  locations: LocationDto[];
  statusByLocationId: Map<string, { severity: Severity; provenance: Provenance; peak?: number; date?: string }>;
  onSelectLocation?: (locationId: string) => void;
}

const INDIA_CENTER: [number, number] = [21.5, 82.0];
const INDIA_ZOOM = 5;

// SVG icons embedded as string templates for Leaflet DivIcon
const ICONS_SVG = {
  SHIELD:
    '<svg class="w-3.5 h-3.5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>',
  WATCH:
    '<svg class="w-3.5 h-3.5 text-slate-900" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
  WARNING:
    '<svg class="w-4 h-4 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
  SEVERE:
    '<svg class="w-4 h-4 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/></svg>',
};

/**
 * Creates custom pulsing Leaflet divIcon depending on severity level.
 * Pulsing ripple is active on WARNING and SEVERE.
 */
function createHazardDivIcon(
  severity: Severity,
  isSelected: boolean,
  shortName: string
): L.DivIcon {
  let markerHtml = '';
  const selectedRingClass = isSelected
    ? 'ring-4 ring-blue-500 ring-offset-2 ring-offset-white dark:ring-offset-slate-900 scale-110 shadow-blue-500/50'
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
 * Controller for smooth map panning when selected location changes.
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
        // Safe fallback
      }
    }
  }, [selectedLocation?.id, shouldPan, map]);

  return null;
};

export const MapView: React.FC<MapViewProps> = ({
  className = '',
  height = '560px',
  locations,
  statusByLocationId,
  onSelectLocation,
}) => {
  const dispatch = useAppDispatch();
  const selectedLocationId = useAppSelector((state) => state.map.selectedLocationId);

  const activeLocation = useMemo(() => {
    return locations.find((l) => l.id === selectedLocationId) || locations[0] || null;
  }, [locations, selectedLocationId]);

  const handleMarkerClick = (loc: LocationDto) => {
    dispatch(setSelectedLocation(loc.id));
    dispatch(setViewport({ center: [loc.latitude, loc.longitude], zoom: 7 }));
    if (onSelectLocation) {
      onSelectLocation(loc.id);
    }
  };

  return (
    <div
      className={`relative w-full rounded-2xl overflow-hidden border border-slate-200 dark:border-[#2A3644] shadow-sm bg-slate-100 dark:bg-[#0b0f19] ${className}`}
      style={{ height: typeof height === 'number' ? `${height}px` : height }}
    >
      {/* Pure Streets Map View - No buttons or clutter over map surface */}
      <MapContainer
        center={INDIA_CENTER}
        zoom={INDIA_ZOOM}
        minZoom={4}
        maxZoom={16}
        scrollWheelZoom={true}
        zoomControl={false}
        className="w-full h-full"
      >
        {/* Streets View Only (OpenStreetMap standard tiles) */}
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          maxZoom={19}
        />

        <ZoomControl position="bottomright" />
        <MapController selectedLocation={activeLocation} shouldPan={true} />

        {/* Location Markers */}
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
                click: () => handleMarkerClick(loc),
              }}
            >
              <Popup className="kestrel-map-popup">
                <div className="p-3 w-64 space-y-2.5 font-sans text-slate-900 dark:text-slate-100">
                  <div className="border-b border-slate-200 dark:border-slate-800 pb-1.5">
                    <div className="text-[10px] text-blue-600 dark:text-blue-400 font-mono">
                      {loc.riverBasin || 'Basin'} · {loc.state}
                    </div>
                    <h3 className="text-xs font-bold leading-snug mt-0.5">{loc.name}</h3>
                  </div>

                  <div className="flex items-center justify-between text-xs">
                    <span className="text-[11px] text-slate-500">Risk Level:</span>
                    <SeverityBadge severity={severity} size="sm" />
                  </div>

                  <div className="flex items-center justify-between text-xs">
                    <span className="text-[11px] text-slate-500">Provenance:</span>
                    <ProvenanceBadge provenance={provenance} size="sm" />
                  </div>

                  <button
                    onClick={() => handleMarkerClick(loc)}
                    className="w-full py-1.5 px-3 rounded-lg text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
                  >
                    <MapPin className="w-3.5 h-3.5" />
                    <span>View Location Details</span>
                  </button>
                </div>
              </Popup>
            </Marker>
          );
        })}
      </MapContainer>
    </div>
  );
};

export default MapView;
