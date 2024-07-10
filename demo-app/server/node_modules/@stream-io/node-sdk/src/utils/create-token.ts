import jwt, { Secret, SignOptions } from 'jsonwebtoken';

export function JWTUserToken(
  apiSecret: Secret,
  payload: { user_id: string; exp: number; iat: number; call_cids?: string[] },
) {
  // make sure we return a clear error when jwt is shimmed (ie. browser build)
  if (jwt == null || jwt.sign == null) {
    throw Error(
      `Unable to find jwt crypto, if you are getting this error is probably because you are trying to generate tokens on browser or React Native (or other environment where crypto functions are not available). Please Note: token should only be generated server-side.`,
    );
  }

  const opts: SignOptions = Object.assign({
    algorithm: 'HS256',
    noTimestamp: true,
  });

  if (payload.iat) {
    opts.noTimestamp = false;
  }
  return jwt.sign(payload, apiSecret, opts);
}

export function JWTServerToken(
  apiSecret: Secret,
  jwtOptions: SignOptions = {},
) {
  const payload = {
    server: true,
  };

  const opts: SignOptions = Object.assign(
    { algorithm: 'HS256', noTimestamp: true },
    jwtOptions,
  );
  return jwt.sign(payload, apiSecret, opts);
}
