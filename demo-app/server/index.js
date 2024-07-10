import express from "express";
import { StreamClient } from '@stream-io/node-sdk';

const app = express();
const PORT = 3000;

// get the api & secret keys here: https://dashboard.getstream.io/app/1324119/video/overview 
const apiKey = '-';
const secret = '-';
const client = new StreamClient(apiKey, secret);

app.get("/api/auth/create-token", (req, res) => {
  const userId = req.query.user_id;
  const exp = Math.round(new Date().getTime() / 1000) + 60; // 1 minute exp
  const token = client.createToken(userId, exp);

  const user = {
    userId: userId,
    apiKey: apiKey,
    token: token,
  };

  const response = JSON.stringify(user)

  console.log(`user token generated:${response}`);

  res.send(response);
});

app.listen(PORT, () => {
  console.log(`Express server running at http://localhost:${PORT}/`);
});
