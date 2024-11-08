const request = require('supertest');
const express = require('express');
const router = require('../user/calcResults');

const app = express();
app.use(express.json());
app.use(router);