import random
import string
import aiosmtplib
from email.message import EmailMessage
from config import EMAIL_ADDRESS, EMAIL_PASSWORD

def generate_otp():
    return ''.join(random.choices(string.digits, k=6))

async def send_otp_email(receiver_email, otp):
    message = EmailMessage()
    message["From"] = EMAIL_ADDRESS
    message["To"] = receiver_email
    message["Subject"] = "Your GitScope OTP Code"

    message.set_content(f"Your OTP is: {otp}")

    await aiosmtplib.send(
        message,
        hostname="smtp.gmail.com",
        port=587,
        start_tls=True,
        username=EMAIL_ADDRESS,
        password=EMAIL_PASSWORD,
    )