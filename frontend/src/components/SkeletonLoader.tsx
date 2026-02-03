import React from 'react';
import './SkeletonLoader.css';

interface SkeletonLoaderProps {
  type?: 'text' | 'title' | 'avatar' | 'thumbnail' | 'card' | 'list';
  count?: number;
  width?: string;
  height?: string;
  borderRadius?: string;
}

const SkeletonLoader: React.FC<SkeletonLoaderProps> = ({
  type = 'text',
  count = 1,
  width,
  height,
  borderRadius,
}) => {
  const getSkeletonStyle = () => {
    const style: React.CSSProperties = {};

    if (width) style.width = width;
    if (height) style.height = height;
    if (borderRadius) style.borderRadius = borderRadius;

    return style;
  };

  const renderSkeleton = () => {
    switch (type) {
      case 'avatar':
        return (
          <div
            className="skeleton skeleton-avatar"
            style={getSkeletonStyle()}
          />
        );

      case 'thumbnail':
        return (
          <div
            className="skeleton skeleton-thumbnail"
            style={getSkeletonStyle()}
          />
        );

      case 'title':
        return (
          <div
            className="skeleton skeleton-title"
            style={getSkeletonStyle()}
          />
        );

      case 'card':
        return (
          <div className="skeleton-card">
            <div className="skeleton skeleton-card-image" />
            <div className="skeleton-card-content">
              <div className="skeleton skeleton-title" />
              <div className="skeleton skeleton-text" />
              <div className="skeleton skeleton-text" style={{ width: '80%' }} />
            </div>
          </div>
        );

      case 'list':
        return (
          <div className="skeleton-list-item">
            <div className="skeleton skeleton-avatar" />
            <div className="skeleton-list-content">
              <div className="skeleton skeleton-title" style={{ width: '60%' }} />
              <div className="skeleton skeleton-text" style={{ width: '80%' }} />
            </div>
          </div>
        );

      case 'text':
      default:
        return (
          <div
            className="skeleton skeleton-text"
            style={getSkeletonStyle()}
          />
        );
    }
  };

  return (
    <>
      {Array.from({ length: count }, (_, index) => (
        <React.Fragment key={index}>{renderSkeleton()}</React.Fragment>
      ))}
    </>
  );
};

export default SkeletonLoader;
