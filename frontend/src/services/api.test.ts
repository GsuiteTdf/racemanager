import { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { afterEach, describe, expect, it } from 'vitest'
import { api } from './api'
import { guardarSesion, obtenerToken } from './sesion'

function iniciarSesionDePrueba() {
  guardarSesion({
    token: 'jwt-de-prueba',
    expiraEn: new Date(Date.now() + 60_000).toISOString(),
    usuario: { id: 1, email: 'm@test.com', nombre: 'M', roles: ['MANAGER'] },
  })
}

function respuesta(config: InternalAxiosRequestConfig, status: number, data: unknown): AxiosResponse {
  return { data, status, statusText: String(status), headers: {}, config }
}

describe('cliente api', () => {
  afterEach(() => sessionStorage.clear())

  it('envía el JWT en el encabezado Authorization', async () => {
    iniciarSesionDePrueba()
    let enviado: unknown
    api.defaults.adapter = async (config) => {
      enviado = config.headers.Authorization
      return respuesta(config, 200, {})
    }
    await api.get('/ligas')
    expect(enviado).toBe('Bearer jwt-de-prueba')
  })

  it('no envía Authorization si no hay sesión', async () => {
    let enviado: unknown = 'sin-valor'
    api.defaults.adapter = async (config) => {
      enviado = config.headers.Authorization
      return respuesta(config, 200, {})
    }
    await api.get('/health')
    expect(enviado).toBeUndefined()
  })

  it('descarta la sesión local ante un 401', async () => {
    iniciarSesionDePrueba()
    api.defaults.adapter = async (config) => {
      throw new AxiosError('No autenticado', 'ERR_BAD_REQUEST', config, null,
        respuesta(config, 401, { error: 'No autenticado' }))
    }
    await expect(api.get('/ligas')).rejects.toBeInstanceOf(AxiosError)
    expect(obtenerToken()).toBeNull()
  })
})
