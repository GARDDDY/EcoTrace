const nodemailer = require('nodemailer');
const logger = require('../logger');

const FROM = process.env.EMAIL_FROM || 'EcoTrace <noreply@localhost>';

const transporter = process.env.SMTP_HOST
    ? nodemailer.createTransport({
        host: process.env.SMTP_HOST,
        port: Number(process.env.SMTP_PORT) || 587,
        secure: process.env.SMTP_SECURE === 'true',
        auth: { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS },
    })
    : null;

function resetCodeHtml(name, code) {
    return `<!DOCTYPE html>
<html lang="ru"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  body { font-family: Arial, sans-serif; background:#f4f4f4; margin:0; padding:20px; }
  .container { max-width:600px; margin:auto; background:#fff; padding:20px; border-radius:5px; box-shadow:0 0 10px rgba(0,0,0,.1); }
  .header { text-align:center; margin-bottom:20px; }
  .code { font-size:24px; font-weight:bold; color:#25633B; text-align:center; display:block; margin:20px 0; }
</style></head>
<body><div class="container">
  <div class="header"><h2>Здравствуйте, ${name}!</h2></div>
  <p>Используйте этот код в приложении EcoTrace для сброса пароля своей учетной записи!</p>
  <p class="code">${code}</p>
</div></body></html>`;
}

async function sendResetCode(email, name, code) {
    if (!transporter) {
        logger.warn('SMTP не настроен — код сброса записан в лог вместо отправки', { email, code });
        return;
    }
    try {
        await transporter.sendMail({
            from: FROM,
            to: email,
            subject: 'Сброс пароля',
            html: resetCodeHtml(name, code),
        });
    } catch (error) {
        logger.error('Failed to send reset code email', { error: error.message });
        throw error;
    }
}

module.exports = { sendResetCode };
