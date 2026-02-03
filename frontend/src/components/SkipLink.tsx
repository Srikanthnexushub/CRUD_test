import React from 'react';
import './SkipLink.css';

interface SkipLinkProps {
  href: string;
  children: string;
}

const SkipLink: React.FC<SkipLinkProps> = ({ href, children }) => {
  return (
    <a href={href} className="skip-link">
      {children}
    </a>
  );
};

export default SkipLink;
