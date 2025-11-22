import Link from 'next/link'

export default function Home() {
  return (
    <div style={{ padding: '2rem', textAlign: 'center' }}>
      <h1>Customer Registration App</h1>
      <div style={{ marginTop: '2rem' }}>
        <Link href="/register" style={{ margin: '1rem', padding: '0.5rem 1rem', background: '#0070f3', color: 'white', textDecoration: 'none' }}>
          Register Customer
        </Link>
        <Link href="/customers" style={{ margin: '1rem', padding: '0.5rem 1rem', background: '#0070f3', color: 'white', textDecoration: 'none' }}>
          View Customers
        </Link>
      </div>
    </div>
  )
}
