let customers = []
let currentId = 1

export default function handler(req, res) {
  const { method } = req

  switch (method) {
    case 'GET':
      handleGet(req, res)
      break
    case 'POST':
      handlePost(req, res)
      break
    case 'DELETE':
      handleDelete(req, res)
      break
    default:
      res.setHeader('Allow', ['GET', 'POST', 'DELETE'])
      res.status(405).end(`Method ${method} Not Allowed`)
  }
}

function handleGet(req, res) {
  res.status(200).json(customers)
}

function handlePost(req, res) {
  const { name, email, phone, address } = req.body

  if (!name || !email || !phone || !address) {
    return res.status(400).json({ error: 'All fields are required' })
  }

  const existingCustomer = customers.find(c => c.email === email)
  if (existingCustomer) {
    return res.status(400).json({ error: 'Email already exists' })
  }

  const newCustomer = {
    id: currentId++,
    name,
    email,
    phone,
    address,
    createdAt: new Date().toISOString()
  }

  customers.push(newCustomer)
  res.status(201).json(newCustomer)
}

function handleDelete(req, res) {
  const { id } = req.query

  if (!id) {
    return res.status(400).json({ error: 'Customer ID is required' })
  }

  const customerIndex = customers.findIndex(c => c.id === parseInt(id))
  
  if (customerIndex === -1) {
    return res.status(404).json({ error: 'Customer not found' })
  }

  customers.splice(customerIndex, 1)
  res.status(200).json({ message: 'Customer deleted successfully' })
}
