import { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

interface ErrorBoundaryProps {
  children: ReactNode;
  fallbackTitle?: string;
  onReset?: () => void;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  public state: ErrorBoundaryState = {
    hasError: false,
    error: null,
  };

  public static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary caught an unhandled error:', error, errorInfo);
  }

  private handleReset = () => {
    this.setState({ hasError: false, error: null });
    if (this.props.onReset) {
      this.props.onReset();
    }
  };

  public render() {
    if (this.state.hasError) {
      return (
        <div className="rounded-2xl border border-red-900/50 bg-red-950/20 p-6 text-slate-100 shadow-xl backdrop-blur-md">
          <div className="flex items-start space-x-3">
            <div className="p-2 rounded-xl bg-red-500/20 border border-red-500/30 text-red-400 mt-0.5">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div className="flex-1 space-y-2">
              <h3 className="text-sm font-semibold text-red-200">
                {this.props.fallbackTitle || 'Component Encountered a Render Error'}
              </h3>
              <p className="text-xs text-slate-400">
                {this.state.error?.message || 'An unexpected error occurred while rendering this component.'}
              </p>
              <button
                onClick={this.handleReset}
                className="mt-2 inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-red-600/30 hover:bg-red-600/50 border border-red-500/40 text-xs font-semibold text-red-200 transition-colors"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>Retry Component</span>
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
