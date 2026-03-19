import * as admin from 'firebase-admin';
import type { VercelRequest, VercelResponse } from '@vercel/node';

// Initialize Firebase Admin based on environment variables
if (!admin.apps.length) {
  try {
    admin.initializeApp({
      credential: admin.credential.cert({
        projectId: process.env.FIREBASE_PROJECT_ID || "gen-lang-client-0120093908",
        clientEmail: process.env.FIREBASE_CLIENT_EMAIL || "firebase-adminsdk-fbsvc@gen-lang-client-0120093908.iam.gserviceaccount.com",
        // Replace literal string \\n with actual newline character for private key
        privateKey: (process.env.FIREBASE_PRIVATE_KEY || "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC1MNT9n5QYcRv4\n4Ogo3Vox5sTSRkuneObNKmvtP4HBVQCeCq/mn1/jlhKz3/LmFkYxHuRzfzvb7GF6\nRumWeCuIqQEHYWdomXy6o2R9r18A5WrcEXGNLBmBgEhoAYYmek/dizQNZDGb5y0F\ngO0m+7GD0yHo6+FSYAzaEdjsThTAL3IVqK7zdFPL20WJEHtfbz009uJRrIBL07TS\nGRkLHqcobqH0v0ctm8BLjFkxiHGqKrsTQpru/zAd+OI1otIf63PnVc1YiIfiObS4\nMtZeWrwsq/8RPeOUGfT8hoZyvpkXsHtAlnjJHtFIZC6GXK/1EOK4n2Bt3J9UrOjG\ngWv7LdADAgMBAAECggEAGYP36oNnao27H7AkYWTSh8Z0nxvxGXNoJSo9KprP3fbn\ncFoW7UYyTa1bkuMNpKMsXUx5ZV5cgprmGz8TM/JWRxdtoqlv04YRz4Kk6yIgIdMs\n2jLGp9e2+8qh56uXRaHhz1QED4K1jbvfVjAStXOqSuMXmmJlR6FpDeiHgLd6KpoG\n3REhiRnJExHzSV/rkC9FiGBbX2CwsAvUTtKapCpHG0lSpDpgDKVZ05YPC5DAkVXg\n1fyaZiPNBtcrdzbpPbx0cSkp2IKWzMGsIA/rhf/JplJ5t1qYaM/joFxTYslSSeQ5\nX3FmrXySkqzvTDcR1R1i6rgEgNwRrhis9ZsuKcuaMQKBgQDu/NhbzZhBKe4ntaO5\n5dGOhGEqgDT485iUNO/baplTXq6PYJ0a+5djHmuJOKG3r3ISjsSqXi+0WFxXzT6I\nGQHf5PDKkEdeKKZn8+Ie2+sMOsx06kg4/xccyP/IdzZftf1lbFSVKbToXYvpt+tt\nuY+rxmpDIkgyAvR6i6dWlga72QKBgQDCFrvID1O0aIjGomCNbzfx4e9ZPJ6cUoe7\nUm+EgXStM6waTW1dSs3mX5aEiJfVhXxEN+SzYZ8aR8+upssTcnauAtluB2dEbXml\nGBWzUr6yWuNk7oC6DIdXA/S0hUM8bU7NfvehIuNR/AdXjbfL+jrJCuynqLXWTjAQ\n2pExWzqNOwKBgQCNolu3TIlHVUpHTmbR7VncVKghQAok4Hk0nIrqRqrHtf1OC7wQ\nGbsGiyjAEkgFTH0WcnYrYdZz00om3wSINAngXxY1dnxVWVIFmYwRLdSHUvGI+LAE\n12kjLhwo+j3HS1v5l9Io9Ka6b3ZT9SQZyzdSXizrzI1s1kN9vPZXYmJrQQKBgHDr\nawsFJ8zxeIogVI5U47adnXRMmXcXaRKp7mmoK6rNQlb3Lya068AZYufu1G+MvvSt\nlChtJnTSzNiu/71rvfKVg8zJD+K20qwjrBRtkrcPu/av8ELOITuV+JvCYJDag5t5\nQDcGpjy/pcHhmMpa5jKrYW5V8J0jVkGn+AtAKXubAoGBAKonzvq4RoGYaN3vycvq\nXXvazB7lCX1NGbXBp3a/bUOg85X4NworTfLp+aZsGh+MIwYVSbWlnfciw8za06FM\n+d2x0abjHSdwQPKXaL12ZdJ48wYB0efLTgPrSeiKW9pghI7A0uGPeFWruhJTEaEz\ntZRt/5kRxACBP4CNbBAwovaR\n-----END PRIVATE KEY-----\n")?.replace(/\\n/g, '\n'),
      }),
    });
  } catch (error) {
    console.error('Firebase Admin initialization error:', error);
  }
}

export default async function handler(req: VercelRequest, res: VercelResponse) {
  // Option to secure the endpoint via a basic Secret key (e.g. from cron-job.org)
  const cronSecret = process.env.CRON_SECRET;
  const authHeader = req.headers.authorization;
  const querySecret = req.query.secret;

  // Let either Authorization Header or ?secret= search param authenticate the cron request
  if (cronSecret && authHeader !== `Bearer ${cronSecret}` && querySecret !== cronSecret) {
    return res.status(401).json({ error: 'Unauthorized request' });
  }

  try {
    const db = admin.firestore();
    const messaging = admin.messaging();

    // The times are saved in "HH:mm" local to the user layout. 
    // Assuming 'Asia/Dhaka' because it's a Bengali app configuration mostly
    const formatter = new Intl.DateTimeFormat('en-US', {
      timeZone: 'Asia/Dhaka', // Adjust this if you want it globally dynamic
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });
    
    // Formatting yields something like "14:30" or "24:30"
    let currentTime = formatter.format(new Date()); 
    if (currentTime.startsWith('24:')) {
      currentTime = currentTime.replace('24:', '00:');
    }

    console.log(`[Cron] Checking scheduled remainders for time: ${currentTime}`);

    const usersSnapshot = await db.collection('users').get();
    const promises: Promise<any>[] = [];

    usersSnapshot.forEach((doc) => {
      const data = doc.data();
      
      // Look for users who have Push enabled, an active token, and reminders established
      if (data.push?.enabled && data.push?.token && data.reminders && Array.isArray(data.reminders)) {
         
         const activeReminders = data.reminders.filter((r: any) => 
           r.enabled && r.time === currentTime
         );

         activeReminders.forEach((reminder: any) => {
           const payload = {
             notification: {
               title: 'হিসাব নিকাশ রিমাইন্ডার',
               body: reminder.message || 'অ্যাপ খুলে আপনার ডেইলি নোটস আপডেট করুন।',
             },
             token: data.push.token,
           };

           // Push the notification request
           const messagingPromise = messaging.send(payload).catch(async (error) => {
             console.error(`Error sending message to ${doc.id}:`, error);
             // If token is dead or invalid, we should clean it from the database to stop failing next time
             if (error.code === 'messaging/registration-token-not-registered' || error.code === 'messaging/invalid-registration-token') {
                await doc.ref.update({
                  'push.enabled': false,
                  'push.token': admin.firestore.FieldValue.delete()
                });
                console.log(`Removed invalid token for user ${doc.id}`);
             }
           });
           
           promises.push(messagingPromise);
         });
      }
    });

    await Promise.allSettled(promises);

    return res.status(200).json({ 
      success: true, 
      message: `Checked and triggered valid remainders for time: ${currentTime}`,
      dispatched: promises.length
    });
    
  } catch (error: any) {
    console.error('Critical Cron Job Error:', error);
    return res.status(500).json({ error: error.message || 'Internal Server Error' });
  }
}
