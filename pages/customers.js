import { useState, useEffect } from 'react'

export default function Customers() {
  const [customers, setCustomers] = useState([])

  useEffect(() => {
    fetchCustomers()
  }, [])

  const fetchCustomers = async () => {
    try {
      const response = await fetch('/api/customers')
      const data = await response.json()
      setCustomers(data)
    } catch (error) {
      console.error('Error fetching customers:', error)
    }
  }

  const deleteCustomer = async (id) => {
    try {
      const response = await fetch(`/api/customers?id=${id}`, {
        method: 'DELETE',
      })

      if (response.ok) {
        fetchCustomers()
        alert('Customer deleted successfully!')
      } else {
        alert('Error deleting customer')
      }
    } catch (error) {
      console.error('Error:', error)
      alert('Error deleting customer')
    }
  }

  return (
    <div style={{ padding: '2rem' }}>
      <h1>Registered Customers</h1>
      {customers.length === 0 ? (
        <p>No customers registered yet.</p>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ backgroundColor: '#f5f5f5' }}>
              <th style={{ padding: '0.5rem', border: '1px solid #ddd' }}>Name</th>
              <th style={{ padding: '0.5rem', border: '1px solid #ddd' }}>Email</th>
              <th style={{ padding: '0.5rem', border: '1px solid #ddd' }}>Phone</th>
              <th style={{ padding: '0.5rem', border: '1px solid #ddd' }}>Address</th>
              <th style={{ padding: '0.5rem', border: '1px solid #ddd' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {customers.map((customer) => (
              <tr key={customer.id}>
                <td style={{ padding: '0.5rem', border: '1px solid #ddd' }}>{customer.name}</td>
                <td style={{ padding: '0.5rem', border: '1px solid #ddd' }}>{customer.email}</td>
                <td style={{ padding: '0.5rem', border: '1px solid #ddd' }}>{customer.phone}</td>
                <td style={{ padding: '0.5rem', border: '1px solid #ddd' }}>{customer.address}</td>
                <td style={{ padding: '0.5rem', border: '1px solid #ddd' }}>
                  <button 
                    onClick={() => deleteCustomer(customer.id)}
                    style={{ padding: '0.25rem 0.5rem', background: '#ff4444', color: 'white', border: 'none' }}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
