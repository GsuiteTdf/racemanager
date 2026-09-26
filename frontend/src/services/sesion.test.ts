import { afterEach, describe, expect, it } from 'vitest'
import { borrarSesion, guardarSesion, obtenerSesion, obtenerToken, type Sesion } from './sesion'

function sesionQueVenceEn(ms: number): Sesion {
  return {
    token: 'token-de-prueba',
    expiraEn: new Date(Date.now() + ms).toISOString(),
    usuario: { id: 1, email: 'manager@test.com', nombre: 'Manager', roles: ['MANAGER'] },
  }
}

describe('sesion', () => {
  afterEach(() => sessionStorage.clear())

  it('guarda y recupera una sesión vigente', () => {
    guardarSesion(sesionQueVenceEn(60_000))
    expect(obtenerToken()).toBe('token-de-prueba')
    expect(obtenerSesion()?.usuario.roles).toEqual(['MANAGER'])
  })

  it('descarta una sesión vencida', () => {
    guardarSesion(sesionQueVenceEn(-1_000))
    expect(obtenerSesion()).toBeNull()
    expect(sessionStorage.length).toBe(0)
  })

  it('ignora contenido corrupto en el almacenamiento', () => {
    sessionStorage.setItem('racemanager.sesion', '{no es json')
    expect(obtenerSesion()).toBeNull()
  })

  it('borrarSesion elimina el token', () => {
    guardarSesion(sesionQueVenceEn(60_000))
    borrarSesion()
    expect(obtenerToken()).toBeNull()
  })
})
