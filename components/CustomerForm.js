import { useState } from 'react'
import { useRouter } from 'next/router'

export default function CustomerForm() {
  const router = useRouter()
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone: '',
    address: ''
  })

  const handleSubmit = async (e) => {
    e.preventDefault()
    
    try {
      const response = await fetch('/api/customers', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      })

      if (response.ok) {
        alert('Customer registered successfully!')
        setFormData({ name: '', email: '', phone: '', address: '' })
        router.push('/customers')
      } else {
        const error = await response.json()
        alert(error.error || 'Error registering customer')
      }
    } catch (error) {
      console.error('Error:', error)
      alert('Error registering customer')
    }
  }

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    })
  }

  return (
    <form onSubmit={handleSubmit}>
      <div style={{ marginBottom: '1rem' }}>
        <label>Name:</label>
        <input
          type="text"
          name="name"
          value={formData.name}
          onChange={handleChange}
          required
          style={{ width: '100%', padding: '0.5rem' }}
        />
      </div>
      
      <div style={{ marginBottom: '1rem' }}>
        <label>Email:</label>
        <input
          type="email"
          name="email"
          value={formData.email}
          onChange={handleChange}
          required
          style={{ width: '100%', padding: '0.5rem' }}
        />
      </div>
      
      <div style={{ marginBottom: '1rem' }}>
        <label>Phone:</label>
        <input
          type="tel"
          name="phone"
          value={formData.phone}
          onChange={handleChange}
          required
          style={{ width: '100%', padding: '0.5rem' }}
        />
      </div>
      
      <div style={{ marginBottom: '1rem' }}>
        <label>Address:</label>
        <textarea
          name="address"
          value={formData.address}
          onChange={handleChange}
          required
          style={{ width: '100%', padding: '0.5rem', minHeight: '100px' }}
        />
      </div>
      
      <button type="submit" style={{ padding: '0.5rem 2rem', background: '#0070f3', color: 'white', border: 'none' }}>
        Register Customer
      </button>
    </form>
  )
}
