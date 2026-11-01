import React, { useEffect, useState } from 'react';
import {
  MapContainer,
  TileLayer,
  Marker,
  useMap,
  ZoomControl,
} from 'react-leaflet';
import L from 'leaflet';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { setSelectedLocation, setViewport } from '../features/map/mapSlice';
import type { LocationDto, Severity, Provenance } from '../types/api';

export const BASIN_OPTIONS = [
  { id: 'ALL', label: 'All Basins' },
  { id: 'Ganga Basin', label: 'Ganga Basin' },
  { id: 'Brahmaputra Basin', label: 'Brahmaputra Basin' },
  { id: 'Indus/North', label: 'Indus & North Himalayan' },
  { id: 'Godavari Basin', label: 'Godavari Basin' },
  { id: 'Krishna Basin', label: 'Krishna Basin' },
  { id: 'Mahanadi Basin', label: 'Mahanadi Basin' },
  { id: 'West-Flowing', label: 'West-Flowing Rivers' },
  { id: 'Southern', label: 'Southern Basins' },
  { id: 'Eastern', label: 'Eastern Basin' },
  { id: 'West Coastal', label: 'West Coastal' },
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
  WATCH:
    '<svg class="w-2.5 h-2.5 text-slate-900" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
  WARNING:
    '<svg class="w-3 h-3 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
  SEVERE:
    '<svg class="w-3 h-3 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/></svg>',
  SAFE_CIRCLE:
    '<span class="w-1.5 h-1.5 rounded-full bg-white shadow-xs"></span>',
};

/**
 * Creates custom pulsing Leaflet divIcon:
 * Flame (Severe), Exclamation Circle (Warning), Triangle (Watch), Clean Circle (Safe - no shield),
 * paired with the dark text badge and glowing colored text styling.
 */
function createHazardDivIcon(
  severity: Severity,
  isSelected: boolean,
  shortName: string
): L.DivIcon {
  const selectedRingClass = isSelected
    ? 'ring-4 ring-blue-500 ring-offset-2 ring-offset-white dark:ring-offset-slate-900 scale-125 shadow-blue-500/60 z-30'
    : '';

  let beaconHtml = '';
  let badgeColorClass = '';

  switch (severity) {
    case 'SEVERE':
      badgeColorClass = 'bg-slate-950/90 text-red-300 border border-red-500/40';
      beaconHtml = `
        <div class="relative flex items-center justify-center">
          <span class="absolute w-7.5 h-7.5 rounded-full bg-red-600/40 animate-ping"></span>
          <span class="absolute w-6.5 h-6.5 rounded-full bg-red-500/30 animate-pulse"></span>
          <div class="relative z-10 w-5.5 h-5.5 rounded-full bg-gradient-to-br from-red-500 to-red-700 border-2 border-red-100 shadow-xl shadow-red-950 flex items-center justify-center text-white transition-transform hover:scale-115 ${selectedRingClass}">
            ${ICONS_SVG.SEVERE}
          </div>
        </div>
      `;
      break;

    case 'WARNING':
      badgeColorClass = 'bg-slate-950/90 text-orange-300 border border-orange-500/40';
      beaconHtml = `
        <div class="relative flex items-center justify-center">
          <span class="absolute w-6.5 h-6.5 rounded-full bg-orange-500/35 animate-ping"></span>
          <div class="relative z-10 w-5 h-5 rounded-full bg-gradient-to-br from-orange-400 to-orange-600 border-2 border-orange-100 shadow-lg shadow-orange-950 flex items-center justify-center text-white transition-transform hover:scale-115 ${selectedRingClass}">
            ${ICONS_SVG.WARNING}
          </div>
        </div>
      `;
      break;

    case 'WATCH':
      badgeColorClass = 'bg-slate-950/90 text-amber-300 border border-amber-500/40';
      beaconHtml = `
        <div class="relative flex items-center justify-center">
          <div class="relative z-10 w-4.5 h-4.5 rounded-full bg-gradient-to-br from-amber-300 to-amber-500 border-2 border-amber-100 shadow-md shadow-amber-950 flex items-center justify-center text-slate-950 transition-transform hover:scale-115 ${selectedRingClass}">
            ${ICONS_SVG.WATCH}
          </div>
        </div>
      `;
      break;

    case 'NONE':
    default:
      badgeColorClass = 'bg-slate-950/90 text-slate-300 border border-slate-700/80';
      beaconHtml = `
        <div class="relative flex items-center justify-center">
          <div class="relative z-10 w-3.5 h-3.5 rounded-full bg-gradient-to-br from-emerald-400 to-emerald-600 border-2 border-emerald-100 shadow-sm shadow-emerald-950 flex items-center justify-center transition-transform hover:scale-115 ${selectedRingClass}">
            ${ICONS_SVG.SAFE_CIRCLE}
          </div>
        </div>
      `;
      break;
  }

  const markerHtml = `
    <div class="relative flex flex-col items-center justify-center w-[130px] group cursor-pointer select-none">
      <div class="h-6 flex items-center justify-center">
        ${beaconHtml}
      </div>
      <div class="mt-1 px-2.5 py-0.5 rounded text-[12px] font-bold tracking-tight whitespace-nowrap shadow-lg group-hover:scale-105 transition-transform pointer-events-none ${badgeColorClass}">
        <span class="truncate max-w-[110px] inline-block">${shortName}</span>
      </div>
    </div>
  `;

  return L.divIcon({
    className: 'custom-hazard-marker',
    html: markerHtml,
    iconSize: [130, 50],
    iconAnchor: [65, 12],
  });
}

/**
 * Controller for map panning ONLY when user explicitly clicks on a location.
 */
interface MapControllerProps {
  panTarget: { lat: number; lng: number } | null;
}

const MapController: React.FC<MapControllerProps> = ({ panTarget }) => {
  const map = useMap();

  useEffect(() => {
    // Only pan when panTarget is explicitly set by user click
    if (panTarget && map) {
      try {
        map.flyTo([panTarget.lat, panTarget.lng], 7, {
          duration: 1.0,
        });
      } catch {
        // Safe fallback
      }
    }
  }, [panTarget, map]);

  return null;
};

const ResizeObserverHandler: React.FC = () => {
  const map = useMap();
  useEffect(() => {
    const timer = setTimeout(() => {
      map.invalidateSize();
    }, 150);
    return () => clearTimeout(timer);
  }, [map]);
  return null;
};

export const MapView: React.FC<MapViewProps> = ({
  className = '',
  height = '100%',
  locations,
  statusByLocationId,
  onSelectLocation,
}) => {
  const dispatch = useAppDispatch();
  const selectedLocationId = useAppSelector((state) => state.map.selectedLocationId);

  // User click-driven pan target (starts null so it NEVER auto-pans on mount/filter changes)
  const [panTarget, setPanTarget] = useState<{ lat: number; lng: number } | null>(null);

  const handleMarkerClick = (loc: LocationDto) => {
    dispatch(setSelectedLocation(loc.id));
    dispatch(setViewport({ center: [loc.latitude, loc.longitude], zoom: 7 }));
    setPanTarget({ lat: loc.latitude, lng: loc.longitude });
    if (onSelectLocation) {
      onSelectLocation(loc.id);
    }
  };

  return (
    <div
      className={`relative z-0 isolate w-full h-full rounded-2xl overflow-hidden border border-slate-200 dark:border-[#2A3644] shadow-sm bg-slate-100 dark:bg-[#0b0f19] ${className}`}
      style={{ height: typeof height === 'number' ? `${height}px` : height }}
    >
      {/* Pure Streets Map View - Light & Dark Streets based on active theme */}
      <MapContainer
        center={INDIA_CENTER}
        zoom={INDIA_ZOOM}
        minZoom={4}
        maxZoom={16}
        scrollWheelZoom={true}
        zoomControl={false}
        className="w-full h-full"
      >
        {/* Streets View Only (OpenStreetMap standard - styled dynamically for light & dark modes) */}
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          maxZoom={19}
        />

        <ZoomControl position="bottomleft" />
        <MapController panTarget={panTarget} />
        <ResizeObserverHandler />

        {/* Location Markers */}
        {locations.map((loc) => {
          const locStatus = statusByLocationId.get(loc.id);
          const severity: Severity = locStatus?.severity || 'NONE';
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
            />
          );
        })}
      </MapContainer>
    </div>
  );
};

export default MapView;
