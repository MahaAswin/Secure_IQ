import React from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Shield, Brain, BarChart3, Monitor, Lock, Users, ChevronRight, Star, ArrowRight, CheckCircle } from 'lucide-react';
import { Button } from '@/components/common/Button';
import { ROUTES, APP_NAME, APP_TAGLINE } from '@/constants';

const features = [
  {
    icon: Brain,
    title: 'AI-Powered Monitoring',
    desc: 'Real-time face detection, gaze tracking, and behavioral analysis with 99.2% accuracy.',
    color: 'text-blue-500',
    bg: 'bg-blue-500/10',
  },
  {
    icon: Shield,
    title: 'Secure Browser',
    desc: 'Lockdown browser environment that prevents tab switching, copy-paste, and external tools.',
    color: 'text-purple-500',
    bg: 'bg-purple-500/10',
  },
  {
    icon: BarChart3,
    title: 'Advanced Analytics',
    desc: 'Comprehensive reports with score distributions, department comparisons, and grade trends.',
    color: 'text-green-500',
    bg: 'bg-green-500/10',
  },
  {
    icon: Monitor,
    title: 'Live Monitoring',
    desc: 'Faculty can monitor all students simultaneously with instant alerts for suspicious behavior.',
    color: 'text-amber-500',
    bg: 'bg-amber-500/10',
  },
  {
    icon: Lock,
    title: 'Role-Based Access',
    desc: 'Separate portals for students, faculty, HODs, and administrators with fine-grained permissions.',
    color: 'text-red-500',
    bg: 'bg-red-500/10',
  },
  {
    icon: Users,
    title: 'Explainable AI',
    desc: 'Transparent AI reports explain every flag with confidence scores and behavioral insights.',
    color: 'text-cyan-500',
    bg: 'bg-cyan-500/10',
  },
];

const stats = [
  { value: '10,000+', label: 'Students Served' },
  { value: '500+', label: 'Exams Conducted' },
  { value: '99.9%', label: 'Platform Uptime' },
  { value: '98%', label: 'Detection Accuracy' },
];

const fadeUp = {
  initial: { opacity: 0, y: 24 },
  whileInView: { opacity: 1, y: 0 },
  viewport: { once: true },
  transition: { duration: 0.5 },
};

export function LandingPage() {
  return (
    <div className="min-h-screen bg-background text-foreground">
      {/* Navbar */}
      <nav className="fixed top-0 left-0 right-0 z-50 border-b border-border/60 bg-background/80 backdrop-blur-md">
        <div className="container max-w-6xl flex items-center justify-between h-16">
          <div className="flex items-center gap-2.5">
            <div className="h-8 w-8 rounded-lg bg-gradient-primary flex items-center justify-center text-white font-bold font-display">
              S
            </div>
            <span className="font-display font-bold text-foreground text-lg">{APP_NAME}</span>
          </div>
          <div className="hidden md:flex items-center gap-6 text-sm text-muted-foreground">
            {['Features', 'About', 'Contact'].map((l) => (
              <a key={l} href={`#${l.toLowerCase()}`} className="hover:text-foreground transition-colors">{l}</a>
            ))}
          </div>
          <Link to={ROUTES.LOGIN}>
            <Button variant="primary" size="sm">
              Sign In <ArrowRight className="h-3.5 w-3.5" />
            </Button>
          </Link>
        </div>
      </nav>

      {/* Hero */}
      <section className="pt-32 pb-20 px-6 relative overflow-hidden">
        <div className="absolute inset-0 bg-hero-mesh pointer-events-none" />
        <div className="container max-w-5xl mx-auto text-center relative z-10">
          <motion.div {...fadeUp}>
            <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/10 text-primary text-xs font-semibold mb-6">
              <span className="h-1.5 w-1.5 rounded-full bg-primary animate-pulse" />
              AI-Powered Examination Platform
            </span>
          </motion.div>
          <motion.h1
            {...fadeUp}
            transition={{ duration: 0.5, delay: 0.1 }}
            className="font-display font-bold text-5xl md:text-6xl lg:text-7xl text-foreground leading-tight text-balance mb-6"
          >
            Secure Exams.{' '}
            <span className="gradient-text">Intelligent</span>{' '}
            Decisions.
          </motion.h1>
          <motion.p
            {...fadeUp}
            transition={{ duration: 0.5, delay: 0.2 }}
            className="text-muted-foreground text-lg md:text-xl max-w-2xl mx-auto mb-10 text-balance"
          >
            {APP_TAGLINE} — SecureIQ combines AI monitoring, real-time analytics, and explainable reports to modernize academic examination.
          </motion.p>
          <motion.div
            {...fadeUp}
            transition={{ duration: 0.5, delay: 0.3 }}
            className="flex flex-col sm:flex-row gap-3 justify-center"
          >
            <Link to={ROUTES.LOGIN}>
              <Button variant="primary" size="xl">
                Get Started <ChevronRight className="h-4 w-4" />
              </Button>
            </Link>
            <Button variant="outline" size="xl">
              View Demo
            </Button>
          </motion.div>

          {/* Hero image mockup */}
          <motion.div
            initial={{ opacity: 0, y: 40 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.7, delay: 0.2 }}
            className="mt-16 relative"
          >
            <div className="rounded-2xl border border-border shadow-elevated overflow-hidden bg-card">
              <div className="flex items-center gap-1.5 px-4 py-3 border-b border-border bg-muted/50">
                <div className="h-2.5 w-2.5 rounded-full bg-danger/60" />
                <div className="h-2.5 w-2.5 rounded-full bg-warning/60" />
                <div className="h-2.5 w-2.5 rounded-full bg-success/60" />
                <div className="flex-1 text-center text-xs text-muted-foreground font-mono">secureiq.edu</div>
              </div>
              <div className="grid grid-cols-4 h-80">
                {/* Sidebar mockup */}
                <div className="border-r border-border bg-card p-4 space-y-2">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <div key={i} className={`h-8 rounded-lg ${i === 0 ? 'bg-primary/10' : 'bg-muted'}`} />
                  ))}
                </div>
                {/* Content mockup */}
                <div className="col-span-3 p-5 space-y-4">
                  <div className="grid grid-cols-4 gap-3">
                    {Array.from({ length: 4 }).map((_, i) => (
                      <div key={i} className="h-20 rounded-xl bg-muted/60" />
                    ))}
                  </div>
                  <div className="grid grid-cols-3 gap-3">
                    <div className="col-span-2 h-32 rounded-xl bg-muted/60" />
                    <div className="h-32 rounded-xl bg-muted/60" />
                  </div>
                </div>
              </div>
            </div>
            {/* Glow */}
            <div className="absolute -inset-4 -z-10 rounded-3xl bg-gradient-to-r from-primary/10 to-secondary/10 blur-3xl" />
          </motion.div>
        </div>
      </section>

      {/* Stats */}
      <section className="py-16 border-y border-border bg-muted/30">
        <div className="container max-w-4xl mx-auto">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
            {stats.map((stat, i) => (
              <motion.div
                key={stat.label}
                initial={{ opacity: 0, y: 16 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                className="text-center"
              >
                <p className="font-display font-bold text-3xl md:text-4xl text-foreground">{stat.value}</p>
                <p className="text-muted-foreground text-sm mt-1">{stat.label}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Features */}
      <section id="features" className="py-20 px-6">
        <div className="container max-w-6xl mx-auto">
          <motion.div {...fadeUp} className="text-center mb-12">
            <h2 className="font-display font-bold text-3xl md:text-4xl text-foreground mb-4">
              Everything you need for{' '}
              <span className="gradient-text">secure examinations</span>
            </h2>
            <p className="text-muted-foreground text-lg max-w-xl mx-auto">
              Built for academic institutions that demand integrity, transparency, and intelligent insights.
            </p>
          </motion.div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {features.map((f, i) => {
              const Icon = f.icon;
              return (
                <motion.div
                  key={f.title}
                  initial={{ opacity: 0, y: 20 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.08 }}
                  className="group p-5 rounded-xl border border-border bg-card hover:shadow-card hover:-translate-y-0.5 transition-all duration-200"
                >
                  <div className={`h-10 w-10 rounded-xl ${f.bg} flex items-center justify-center mb-4`}>
                    <Icon className={`h-5 w-5 ${f.color}`} />
                  </div>
                  <h3 className="font-display font-semibold text-foreground text-base mb-2">{f.title}</h3>
                  <p className="text-muted-foreground text-sm leading-relaxed">{f.desc}</p>
                </motion.div>
              );
            })}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-20 px-6">
        <div className="container max-w-3xl mx-auto text-center">
          <motion.div {...fadeUp}>
            <div className="rounded-2xl bg-gradient-primary p-10 relative overflow-hidden">
              <div className="absolute inset-0 hero-mesh opacity-40" />
              <div className="relative z-10">
                <h2 className="font-display font-bold text-white text-3xl md:text-4xl mb-4 text-balance">
                  Ready to modernize your examination process?
                </h2>
                <p className="text-blue-100 mb-8">
                  Join hundreds of institutions using SecureIQ for trusted, AI-monitored examinations.
                </p>
                <Link to={ROUTES.LOGIN}>
                  <Button variant="outline" size="lg" className="border-white/30 text-white hover:bg-white/10">
                    Get Started Free <ArrowRight className="h-4 w-4" />
                  </Button>
                </Link>
              </div>
            </div>
          </motion.div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-border py-8 px-6">
        <div className="container max-w-6xl mx-auto flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <div className="h-7 w-7 rounded-lg bg-gradient-primary flex items-center justify-center text-white font-bold text-sm font-display">S</div>
            <span className="font-display font-semibold text-foreground">{APP_NAME}</span>
          </div>
          <p className="text-sm text-muted-foreground">© {new Date().getFullYear()} SecureIQ. All rights reserved.</p>
        </div>
      </footer>
    </div>
  );
}
