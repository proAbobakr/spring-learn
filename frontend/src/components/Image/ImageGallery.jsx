import React, { useState } from 'react';
import { FaTimes, FaChevronLeft, FaChevronRight } from 'react-icons/fa';
import imageService from '../../services/imageService';
import './ImageGallery.css';

const ImageGallery = ({ images }) => {
  const [selectedIndex, setSelectedIndex] = useState(null);

  if (!images || images.length === 0) return null;

  const openLightbox = (index) => {
    setSelectedIndex(index);
  };

  const closeLightbox = () => {
    setSelectedIndex(null);
  };

  const goToPrevious = () => {
    setSelectedIndex((prev) => (prev > 0 ? prev - 1 : images.length - 1));
  };

  const goToNext = () => {
    setSelectedIndex((prev) => (prev < images.length - 1 ? prev + 1 : 0));
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Escape') closeLightbox();
    if (e.key === 'ArrowLeft') goToPrevious();
    if (e.key === 'ArrowRight') goToNext();
  };

  return (
    <>
      <div className="image-gallery">
        {images.map((image, index) => (
          <div
            key={image.id}
            className="gallery-item"
            onClick={() => openLightbox(index)}
          >
            <img
              src={imageService.getImageUrl(image.fileName)}
              alt={image.caption || 'Location image'}
              loading="lazy"
            />
            {image.caption && <div className="image-caption">{image.caption}</div>}
          </div>
        ))}
      </div>

      {selectedIndex !== null && (
        <div
          className="lightbox"
          onClick={closeLightbox}
          onKeyDown={handleKeyDown}
          tabIndex={0}
        >
          <button className="lightbox-close" onClick={closeLightbox}>
            <FaTimes />
          </button>
          <button className="lightbox-prev" onClick={(e) => { e.stopPropagation(); goToPrevious(); }}>
            <FaChevronLeft />
          </button>
          <button className="lightbox-next" onClick={(e) => { e.stopPropagation(); goToNext(); }}>
            <FaChevronRight />
          </button>
          <div className="lightbox-content" onClick={(e) => e.stopPropagation()}>
            <img
              src={imageService.getImageUrl(images[selectedIndex].fileName)}
              alt={images[selectedIndex].caption || 'Location image'}
            />
            {images[selectedIndex].caption && (
              <div className="lightbox-caption">{images[selectedIndex].caption}</div>
            )}
          </div>
          <div className="lightbox-counter">
            {selectedIndex + 1} / {images.length}
          </div>
        </div>
      )}
    </>
  );
};

export default ImageGallery;
