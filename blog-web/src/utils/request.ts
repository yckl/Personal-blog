import axios from 'axios'

const request = axios.create({
  baseURL: 'http://localhost:8088',
  timeout: 15000,
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      return Promise.reject(new Error(res.message || 'Request failed'))
    }
    return res
  },
  (error) => Promise.reject(error)
)

export default request
