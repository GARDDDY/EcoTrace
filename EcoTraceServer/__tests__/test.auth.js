const { crypt, verify } = require('../tech/tAuth');

jest.mock('../server', () => ({
  users: {
    execute: jest.fn(),
  },
  calculator: {
    execute: jest.fn(),
  },
}));

describe('Authentication Tests', () => {
  describe('verify function', () => {

    it('should return true for valid token', () => {
      const validToken = crypt(JSON.stringify({
        type: 1,
        given: Date.now(),
        expires: 100000,
        uid: 'user123',
      }));

      const result = verify('user123', validToken);

      expect(result).toBe(true);
    });

    it('should return false for invalid token (wrong userId)', () => {
      const invalidToken = crypt(JSON.stringify({
        type: 1,
        given: Date.now(),
        expires: 100000,
        uid: 'user123',
      }));

      const result = verify('user456', invalidToken);

      expect(result).toBe(false);
    });

    it('should return false for invalid token (expired)', () => {
      const expiredToken = crypt(JSON.stringify({
        type: 1,
        given: Date.now() - 200000,
        expires: 100000,
        uid: 'user123',
      }));

      const result = verify('user123', expiredToken);

      expect(result).toBe(false);
    });

    it('should return false for invalid token (malformed)', () => {
      const malformedToken = 'invalid-token';

      const result = verify('user123', malformedToken);

      expect(result).toBe(false);
    });
  });
});
