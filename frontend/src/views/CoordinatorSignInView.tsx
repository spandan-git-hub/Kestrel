import React, { useState } from 'react';
import { useAppDispatch } from '../app/hooks';
import { setCoordinatorTab, addToast } from '../features/ui/uiSlice';
import { setCoordinatorToken } from '../features/auth/authSlice';

export const CoordinatorSignInView: React.FC = () => {
  const dispatch = useAppDispatch();

  const [email, setEmail] = useState<string>('anil.kamble@pune-eoc.gov.in');
  const [password, setPassword] = useState<string>('••••••••••••');
  const [showPassword, setShowPassword] = useState<boolean>(false);
  const [rememberTerminal, setRememberTerminal] = useState<boolean>(true);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      dispatch(setCoordinatorToken(`coord_sess_${Date.now()}`));
      dispatch(
        addToast({
          id: `login_${Date.now()}`,
          type: 'success',
          message: 'Welcome Anil Kamble! Authenticated to Pune District EOC session.',
        })
      );
      dispatch(setCoordinatorTab('map'));
    }, 800);
  };

  return (
    <div className="w-full flex-1 flex items-center justify-center py-8 px-4">
      <div className="w-full max-w-[440px] space-y-4">
        {/* Elevated Civic Card */}
        <div className="w-full bg-white dark:bg-[#151D27] border border-slate-200 dark:border-[#2A3644] rounded-2xl p-8 shadow-xl relative space-y-6">
          {/* Header */}
          <div className="flex flex-col items-center text-center space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-blue-50 dark:bg-[#1C2633] border border-blue-200 dark:border-[#2A3644] flex items-center justify-center text-blue-600 dark:text-[#7AA7FF] shadow-xs">
              <span className="material-symbols-outlined text-3xl">flood</span>
            </div>

            <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-slate-100 dark:bg-[#1C2633] text-slate-700 dark:text-[#9AA8B8] text-xs border border-slate-200 dark:border-[#2A3644]">
              <span className="material-symbols-outlined text-sm text-blue-600 dark:text-[#7AA7FF]">verified_user</span>
              <span>Coordinator Ops Portal</span>
            </div>

            <h1 className="text-2xl font-bold text-slate-900 dark:text-[#EAF0F6] tracking-tight">
              Coordinator Sign In
            </h1>
            <p className="text-xs text-slate-500 dark:text-[#9AA8B8] leading-relaxed">
              Sign in with your district emergency operations or municipal ward coordinator credentials.
            </p>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4 text-xs">
            {/* Email / ID */}
            <div className="space-y-1">
              <label className="font-semibold text-slate-900 dark:text-[#EAF0F6] flex justify-between">
                <span>Official email or Gov/EOC ID</span>
                <span className="text-slate-400 dark:text-[#9AA8B8] font-normal">NIC / PMRDA</span>
              </label>
              <input
                type="text"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="w-full h-11 px-3.5 bg-white dark:bg-[#0E1520] border border-slate-300 dark:border-[#2A3644] rounded-xl text-slate-900 dark:text-[#EAF0F6] text-xs focus:border-blue-500 dark:focus:border-[#7AA7FF] focus:outline-none"
              />
            </div>

            {/* Password */}
            <div className="space-y-1">
              <label className="font-semibold text-slate-900 dark:text-[#EAF0F6]">Password</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  className="w-full h-11 pl-3.5 pr-10 bg-white dark:bg-[#0E1520] border border-slate-300 dark:border-[#2A3644] rounded-xl text-slate-900 dark:text-[#EAF0F6] text-xs focus:border-blue-500 dark:focus:border-[#7AA7FF] focus:outline-none"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-3 text-slate-400 hover:text-slate-700 dark:text-[#9AA8B8] dark:hover:text-white"
                >
                  <span className="material-symbols-outlined text-lg">
                    {showPassword ? 'visibility_off' : 'visibility'}
                  </span>
                </button>
              </div>
            </div>

            {/* Remember & Forgot */}
            <div className="flex items-center justify-between pt-1">
              <label className="flex items-center gap-2 cursor-pointer text-slate-600 dark:text-[#9AA8B8]">
                <input
                  type="checkbox"
                  checked={rememberTerminal}
                  onChange={(e) => setRememberTerminal(e.target.checked)}
                  className="accent-blue-600 rounded"
                />
                <span>Remember this terminal (7 days)</span>
              </label>
              <a href="#" className="text-blue-600 dark:text-[#7AA7FF] hover:underline">
                Forgot password?
              </a>
            </div>

            {/* Submit CTA */}
            <div className="pt-2">
              <button
                type="submit"
                disabled={isLoading}
                className="w-full h-11 bg-[#1D4ED8] hover:bg-blue-600 text-white font-bold text-xs rounded-xl flex items-center justify-center gap-2 shadow-md shadow-blue-500/20 transition-all active:scale-[0.98] disabled:opacity-50"
              >
                {isLoading ? (
                  <>
                    <span className="material-symbols-outlined text-sm animate-spin">refresh</span>
                    <span>Verifying Credentials...</span>
                  </>
                ) : (
                  <>
                    <span>Sign In to EOC Terminal</span>
                    <span className="material-symbols-outlined text-sm">arrow_forward</span>
                  </>
                )}
              </button>
            </div>
          </form>

          {/* Civic Notice Callout */}
          <div className="pt-4 border-t border-slate-100 dark:border-[#2A3644] text-[11px] text-slate-500 dark:text-[#9AA8B8] flex items-start gap-2">
            <span className="material-symbols-outlined text-blue-600 dark:text-[#7AA7FF] text-base shrink-0 mt-0.5">info</span>
            <div>
              <strong className="text-slate-900 dark:text-[#EAF0F6]">Civic notice:</strong> Kestrel relays early guidance, not statutory emergency directives. For legal evacuation orders, refer to Pune District Magistrate / NDMA.
            </div>
          </div>
        </div>

        {/* Footer Support Info */}
        <p className="text-center text-xs text-slate-500 dark:text-[#9AA8B8]">
          Need EOC access? Contact{' '}
          <a href="#" className="text-blue-600 dark:text-[#7AA7FF] underline">
            Pune District Disaster Management Cell
          </a>{' '}
          or your Ward Supervisor.
        </p>
      </div>
    </div>
  );
};
