import React, { useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import { removeToast } from '../features/ui/uiSlice';
import { CheckCircle2, AlertTriangle, AlertCircle, Info, X } from 'lucide-react';

export const ToastContainer: React.FC = () => {
  const dispatch = useAppDispatch();
  const toasts = useAppSelector((state) => state.ui.toasts);

  return (
    <div className="fixed bottom-5 right-5 z-50 flex flex-col space-y-2 pointer-events-none max-w-sm w-full px-4">
      {toasts.map((toast) => (
        <ToastItem key={toast.id} toast={toast} onDismiss={() => dispatch(removeToast(toast.id))} />
      ))}
    </div>
  );
};

interface ToastItemProps {
  toast: {
    id: string;
    type: 'info' | 'success' | 'warning' | 'error';
    message: string;
  };
  onDismiss: () => void;
}

const ToastItem: React.FC<ToastItemProps> = ({ toast, onDismiss }) => {
  useEffect(() => {
    const timer = setTimeout(() => {
      onDismiss();
    }, 4500);
    return () => clearTimeout(timer);
  }, [onDismiss]);

  const getStyle = () => {
    switch (toast.type) {
      case 'success':
        return {
          icon: CheckCircle2,
          border: 'border-emerald-300 dark:border-emerald-500/40',
          bg: 'bg-emerald-50/95 dark:bg-emerald-950/90 text-emerald-900 dark:text-emerald-200',
          iconCol: 'text-emerald-600 dark:text-emerald-400',
        };
      case 'warning':
        return {
          icon: AlertTriangle,
          border: 'border-amber-300 dark:border-amber-500/40',
          bg: 'bg-amber-50/95 dark:bg-amber-950/90 text-amber-900 dark:text-amber-200',
          iconCol: 'text-amber-600 dark:text-amber-400',
        };
      case 'error':
        return {
          icon: AlertCircle,
          border: 'border-rose-300 dark:border-rose-500/40',
          bg: 'bg-rose-50/95 dark:bg-rose-950/90 text-rose-900 dark:text-rose-200',
          iconCol: 'text-rose-600 dark:text-rose-400',
        };
      case 'info':
      default:
        return {
          icon: Info,
          border: 'border-blue-300 dark:border-blue-500/40',
          bg: 'bg-blue-50/95 dark:bg-blue-950/90 text-blue-900 dark:text-blue-200',
          iconCol: 'text-blue-600 dark:text-blue-400',
        };
    }
  };

  const style = getStyle();
  const Icon = style.icon;

  return (
    <div
      className={`pointer-events-auto flex items-start space-x-2.5 p-3.5 rounded-xl border ${style.border} ${style.bg} backdrop-blur-md shadow-2xl transition-all duration-300 animate-in slide-in-from-bottom-3`}
    >
      <Icon className={`w-4 h-4 shrink-0 mt-0.5 ${style.iconCol}`} />
      <div className="flex-1 text-xs leading-relaxed break-words">{toast.message}</div>
      <button
        onClick={onDismiss}
        className="text-slate-400 hover:text-slate-700 dark:hover:text-white transition-colors p-0.5"
        aria-label="Dismiss toast"
      >
        <X className="w-3.5 h-3.5" />
      </button>
    </div>
  );
};

export default ToastContainer;
