import React from 'react';

export const Logo = ({ className = "w-12 h-12" }: { className?: string }) => (
  <img 
    src="https://ais-dev-4v56xcq727dtrr2z3r4xyn-618498660974.asia-east1.run.app/logo.png" 
    alt="Ultra Optimize X Logo" 
    className={className}
    referrerPolicy="no-referrer"
    onError={(e) => {
      // Fallback if image fails to load
      e.currentTarget.src = "https://picsum.photos/seed/ultra/200/200";
    }}
  />
);
