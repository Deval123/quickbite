import SockJS from "sockjs-client";
import { Stomp } from "@stomp/stompjs";

const ORDER_ID = "ORDER-001";

const socket = new SockJS("http://localhost:8087/ws/tracking");
const client = Stomp.over(socket);

client.connect({}, () => {
    console.log("STOMP connecte !");
    console.log(`Abonne a /topic/delivery/${ORDER_ID}`);
    console.log("En attente de positions GPS...\n");

    client.subscribe(`/topic/delivery/${ORDER_ID}`, (message) => {
        const data = JSON.parse(message.body);
        console.log(`Position recue : lat=${data.latitude}, lon=${data.longitude}, orderId=${data.orderId}`);
    });
});

client.onStompError = (frame) => {
    console.error("Erreur STOMP:", frame.headers["message"]);
};
