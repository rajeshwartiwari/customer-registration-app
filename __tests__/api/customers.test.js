let customers = []
let currentId = 1

const handleGet = (req, res) => {
  res.status(200).json(customers)
}

const handlePost = (req, res) => {
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

const handleDelete = (req, res) => {
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

describe('Customer API', () => {
  beforeEach(() => {
    customers = []
    currentId = 1
  })

  test('GET /api/customers returns empty array initially', () => {
    const req = {}
    const res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn()
    }

    handleGet(req, res)

    expect(res.status).toHaveBeenCalledWith(200)
    expect(res.json).toHaveBeenCalledWith([])
  })

  test('POST /api/customers creates new customer', () => {
    const req = {
      body: {
        name: 'John Doe',
        email: 'john@example.com',
        phone: '1234567890',
        address: '123 Main St'
      }
    }
    const res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn()
    }

    handlePost(req, res)

    expect(res.status).toHaveBeenCalledWith(201)
    expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
      name: 'John Doe',
      email: 'john@example.com',
      phone: '1234567890',
      address: '123 Main St'
    }))
  })

  test('POST /api/customers returns error for missing fields', () => {
    const req = {
      body: {
        name: 'John Doe',
      }
    }
    const res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn()
    }

    handlePost(req, res)

    expect(res.status).toHaveBeenCalledWith(400)
    expect(res.json).toHaveBeenCalledWith({ error: 'All fields are required' })
  })

  test('DELETE /api/customers deletes customer', () => {
    const customer = {
      id: 1,
      name: 'John Doe',
      email: 'john@example.com',
      phone: '1234567890',
      address: '123 Main St',
      createdAt: new Date().toISOString()
    }
    customers.push(customer)

    const req = {
      query: { id: '1' }
    }
    const res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn()
    }

    handleDelete(req, res)

    expect(res.status).toHaveBeenCalledWith(200)
    expect(res.json).toHaveBeenCalledWith({ message: 'Customer deleted successfully' })
    expect(customers).toHaveLength(0)
  })

  test('DELETE /api/customers returns error for non-existent customer', () => {
    const req = {
      query: { id: '999' }
    }
    const res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn()
    }

    handleDelete(req, res)

    expect(res.status).toHaveBeenCalledWith(404)
    expect(res.json).toHaveBeenCalledWith({ error: 'Customer not found' })
  })
}
