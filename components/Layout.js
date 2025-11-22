import Head from 'next/head'
import Link from 'next/link'

export default function Layout({ children }) {
  return (
    <>
      <Head>
        <title>Customer Registration App</title>
        <meta name="description" content="Customer registration microservice" />
        <link rel="icon" href="/favicon.ico" />
      </Head>
      <nav style={{ padding: '1rem', backgroundColor: '#f8f9fa', borderBottom: '1px solid #dee2e6' }}>
        <Link href="/" style={{ marginRight: '1rem', textDecoration: 'none', color: '#0070f3' }}>
          Home
        </Link>
        <Link href="/register" style={{ marginRight: '1rem', textDecoration: 'none', color: '#0070f3' }}>
          Register
        </Link>
        <Link href="/customers" style={{ textDecoration: 'none', color: '#0070f3' }}>
          Customers
        </Link>
      </nav>
      <main>{children}</main>
    </>
  )
}
