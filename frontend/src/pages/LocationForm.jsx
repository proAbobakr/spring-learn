import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useDropzone } from 'react-dropzone';
import { FaUpload, FaTimes } from 'react-icons/fa';
import locationService from '../services/locationService';
import imageService from '../services/imageService';
import { LOCATION_CATEGORIES } from '../utils/constants';
import { toast } from 'react-toastify';
import './LocationForm.css';

const LocationForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEditMode = !!id;

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    category: 'RESTAURANT',
    address: '',
    city: '',
    country: '',
    postalCode: '',
    latitude: 0,
    longitude: 0,
  });

  const [images, setImages] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isEditMode) {
      fetchLocation();
    }
  }, [id]);

  const fetchLocation = async () => {
    try {
      const data = await locationService.getLocationById(id);
      setFormData({
        name: data.name,
        description: data.description,
        category: data.category,
        address: data.address,
        city: data.city,
        country: data.country,
        postalCode: data.postalCode,
        latitude: data.latitude,
        longitude: data.longitude,
      });
    } catch (error) {
      toast.error('Failed to load location');
      navigate('/');
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: name === 'latitude' || name === 'longitude' ? parseFloat(value) || 0 : value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      let locationId;

      if (isEditMode) {
        await locationService.updateLocation(id, formData);
        locationId = id;
        toast.success('Location updated successfully!');
      } else {
        const newLocation = await locationService.createLocation(formData);
        locationId = newLocation.id;
        toast.success('Location created successfully!');
      }

      // Upload images if any
      if (images.length > 0) {
        await uploadImages(locationId);
      }

      navigate(`/locations/${locationId}`);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to save location');
    } finally {
      setLoading(false);
    }
  };

  const uploadImages = async (locationId) => {
    const uploadPromises = images.map((imageFile) =>
      imageService.uploadImage(imageFile, locationId, imageFile.caption || '')
    );

    try {
      await Promise.all(uploadPromises);
      toast.success('Images uploaded successfully!');
    } catch (error) {
      toast.error('Some images failed to upload');
    }
  };

  const onDrop = (acceptedFiles) => {
    const newImages = acceptedFiles.map((file) =>
      Object.assign(file, {
        preview: URL.createObjectURL(file),
        caption: '',
      })
    );
    setImages((prev) => [...prev, ...newImages]);
  };

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'image/*': ['.jpeg', '.jpg', '.png', '.gif', '.webp'],
    },
    multiple: true,
  });

  const removeImage = (index) => {
    setImages((prev) => prev.filter((_, i) => i !== index));
  };

  const updateImageCaption = (index, caption) => {
    setImages((prev) =>
      prev.map((img, i) => (i === index ? { ...img, caption } : img))
    );
  };

  return (
    <div className="location-form-page">
      <div className="form-container">
        <h1>{isEditMode ? 'Edit Location' : 'Add New Location'}</h1>

        <form onSubmit={handleSubmit} className="location-form">
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="name">Location Name *</label>
              <input
                type="text"
                id="name"
                name="name"
                value={formData.name}
                onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="category">Category *</label>
              <select
                id="category"
                name="category"
                value={formData.category}
                onChange={handleChange}
                required
              >
                {LOCATION_CATEGORIES.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat.charAt(0) + cat.slice(1).toLowerCase()}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="description">Description *</label>
            <textarea
              id="description"
              name="description"
              value={formData.description}
              onChange={handleChange}
              rows="5"
              required
            />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="address">Address *</label>
              <input
                type="text"
                id="address"
                name="address"
                value={formData.address}
                onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="city">City *</label>
              <input
                type="text"
                id="city"
                name="city"
                value={formData.city}
                onChange={handleChange}
                required
              />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="country">Country *</label>
              <input
                type="text"
                id="country"
                name="country"
                value={formData.country}
                onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="postalCode">Postal Code</label>
              <input
                type="text"
                id="postalCode"
                name="postalCode"
                value={formData.postalCode}
                onChange={handleChange}
              />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="latitude">Latitude *</label>
              <input
                type="number"
                id="latitude"
                name="latitude"
                value={formData.latitude}
                onChange={handleChange}
                step="0.000001"
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="longitude">Longitude *</label>
              <input
                type="number"
                id="longitude"
                name="longitude"
                value={formData.longitude}
                onChange={handleChange}
                step="0.000001"
                required
              />
            </div>
          </div>

          {!isEditMode && (
            <div className="form-group">
              <label>Images</label>
              <div {...getRootProps()} className={`dropzone ${isDragActive ? 'active' : ''}`}>
                <input {...getInputProps()} />
                <FaUpload />
                <p>
                  {isDragActive
                    ? 'Drop images here...'
                    : 'Drag & drop images here, or click to select'}
                </p>
              </div>

              {images.length > 0 && (
                <div className="image-previews">
                  {images.map((file, index) => (
                    <div key={index} className="image-preview">
                      <img src={file.preview} alt={`Preview ${index + 1}`} />
                      <button
                        type="button"
                        onClick={() => removeImage(index)}
                        className="remove-image"
                      >
                        <FaTimes />
                      </button>
                      <input
                        type="text"
                        placeholder="Add caption..."
                        value={file.caption}
                        onChange={(e) => updateImageCaption(index, e.target.value)}
                        className="caption-input"
                      />
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          <div className="form-actions">
            <button type="submit" className="btn-submit" disabled={loading}>
              {loading ? 'Saving...' : isEditMode ? 'Update Location' : 'Create Location'}
            </button>
            <button
              type="button"
              onClick={() => navigate(-1)}
              className="btn-cancel"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default LocationForm;
