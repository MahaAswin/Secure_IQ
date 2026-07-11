import React from 'react';
import { Outlet } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';

export function AuthLayout() {
  return (
    <div className="min-h-screen bg-background flex">
      {/* Left panel – branding */}
      <div className="hidden lg:flex lg:w-1/2 xl:w-3/5 relative overflow-hidden">
        {/* Gradient background */}
        <div className="absolute inset-0 bg-gradient-dark" />
        {/* Mesh overlay */}
        <div className="absolute inset-0 hero-mesh opacity-60" />
        {/* Grid pattern */}
        <div
          className="absolute inset-0 opacity-5"
          style={{
            backgroundImage: `linear-gradient(rgba(255,255,255,0.1) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.1) 1px, transparent 1px)`,
            backgroundSize: '40px 40px',
          }}
        />

        {/* Content */}
        <div className="relative flex flex-col justify-between p-12 z-10 w-full">
          {/* Logo */}
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-primary text-white font-bold text-lg font-display">
              S
            </div>
            <div>
              <p className="font-display font-bold text-white text-xl">SecureIQ</p>
              <p className="text-[11px] text-blue-300">Secure Exams. Intelligent Decisions.</p>
            </div>
          </div>

          {/* Hero copy */}
          <div className="space-y-6">
            <div className="space-y-3">
              <h1 className="font-display font-bold text-white text-4xl xl:text-5xl leading-tight text-balance">
                AI-Powered<br />
                <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-purple-400">
                  Secure Examination
                </span>
              </h1>
              <p className="text-blue-200 text-base max-w-md">
                Real-time AI monitoring, explainable reports, and comprehensive analytics — all in one platform designed for academic integrity.
              </p>
            </div>

            {/* Feature pills */}
            <div className="flex flex-wrap gap-2">
              {['AI Proctoring', 'Role-Based Access', 'Real-Time Alerts', 'Explainable AI', 'Analytics'].map((f) => (
                <span
                  key={f}
                  className="inline-flex items-center px-3 py-1 rounded-full bg-white/10 backdrop-blur-sm border border-white/20 text-white text-xs font-medium"
                >
                  {f}
                </span>
              ))}
            </div>
          </div>

          {/* Stats */}
          <div className="grid grid-cols-3 gap-4">
            {[
              { value: '10K+', label: 'Students' },
              { value: '500+', label: 'Exams Conducted' },
              { value: '99.9%', label: 'Uptime' },
            ].map((stat) => (
              <div key={stat.label} className="text-center">
                <p className="font-display font-bold text-white text-2xl">{stat.value}</p>
                <p className="text-blue-300 text-xs mt-0.5">{stat.label}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Right panel – form */}
      <div className="flex flex-1 items-center justify-center p-6 lg:p-12">
        <div className="w-full max-w-md">
          {/* Mobile logo */}
          <div className="flex items-center gap-3 mb-8 lg:hidden">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-primary text-white font-bold font-display">
              S
            </div>
            <p className="font-display font-bold text-foreground text-lg">SecureIQ</p>
          </div>
          <Outlet />
        </div>
      </div>

      <Toaster position="top-right" />
    </div>
  );
}
