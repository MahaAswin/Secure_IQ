import React from 'react';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { Home, ArrowLeft } from 'lucide-react';
import { Button } from '@/components/common/Button';
import { ROUTES } from '@/constants';

export function NotFoundPage() {
  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-6">
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        className="text-center max-w-md"
      >
        <div className="font-display font-bold text-8xl text-primary/20 mb-4 select-none">404</div>
        <h1 className="font-display font-bold text-foreground text-2xl mb-2">Page Not Found</h1>
        <p className="text-muted-foreground mb-8">
          The page you're looking for doesn't exist or has been moved.
        </p>
        <div className="flex gap-3 justify-center">
          <Link to={ROUTES.ROOT}>
            <Button variant="primary" leftIcon={<Home className="h-4 w-4" />}>
              Go Home
            </Button>
          </Link>
          <Button variant="outline" onClick={() => history.back()} leftIcon={<ArrowLeft className="h-4 w-4" />}>
            Go Back
          </Button>
        </div>
      </motion.div>
    </div>
  );
}
