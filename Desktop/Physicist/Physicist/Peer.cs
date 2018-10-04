﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Net;
using System.Threading;
using System.Net.Sockets;

namespace Physicist
{
    public class Peer
    {
        const int intervaloTimer = 1000;
        private static System.Threading.Timer timer;
        public static Task<UdpReceiveResult> broadcasting;
        public static Task<int> respostaBroadcasting;
        private const int portaBroadcast = 1729;
        private const string msgReq = "Requisitando";
        private const string msgResp = "Respondendo";
        private static IPAddress endMulticast = IPAddress.Parse("236.0.0.0");
        private static byte[] msgRespBytes = (byte[])Encoding.ASCII.GetBytes(msgResp);
        private IPAddress IP;
        private IPEndPoint meuEnd;
        static UdpClient servidorBroadcast;
        String nome;
        private static IPEndPoint iPConectando;
        public String ToString()
        {
            return this.IP.ToString();
        }
        public IPEndPoint IPConectando{
            get
            {
                return iPConectando;
            }
            
        }
        private void inicializarTimer(int indTarefa)
        {//não tenho ctz se ele é async ou não
            //indTarefa = 0 => broadcasting
            //indTarefa = 1 => respostaBroadcasting
            //estado inicial falso, ficará true caso chegar no estado callback
            var autoEvento = new AutoResetEvent(false);
            var checador = new ChecadorStatus(indTarefa);
            timer = new Timer(checador.CheckStatus, autoEvento, 0, intervaloTimer);
            //autoEvento.WaitOne();
            //liberar e reiniciar o timer
            //"flag" autoevento foi alterada

            /*timer.Dispose();
	        throw new Exception("Timer Peer acabou!");*/
        }
        public static void finalizarTimer() {
            switch (estadoRespostaBroadcasting())
            {
                case TaskStatus.Faulted:
                    finalizarBroadcasting();
                    //throw new Exception("Caso aparentemente impossível!");
                    break;
                case TaskStatus.RanToCompletion:
                    //deu tudo certo!!!
                    //não dar dispose!!
                    //tratar Broadcasting!
                    //throw new Exception("A");
                    tratarBroadcast();
                    break;
                case TaskStatus.WaitingForChildrenToComplete:
                    finalizarBroadcasting();
                    //throw new Exception("Comportamento inesperado da Task");
                    break;
                case TaskStatus.Canceled:

                    finalizarBroadcasting();
                    //throw new Exception("Task mal inicializada!");
                    break;
                default:
                    finalizarBroadcasting();
                    //throw new Exception("Comportamento inesperado do Timer");
                    break;
            }

        }

        private static void tratarBroadcast() {
            //não sei se isso deve ser realmente assíncrono
            IPEndPoint IPQuemMandou;
            UdpReceiveResult req;
            //CancellationToken token = new CancellationToken(true);

            //receber();
            //byte[] datagrama = servidorBroadcast.ReceiveAsync(/*ref IPQuemMandou*/);
            req = broadcasting.Result;
            string requisição = Encoding.ASCII.GetString(req.Buffer);
            IPQuemMandou = req.RemoteEndPoint;

            finalizarBroadcasting();
            if (!requisição.Equals("Requisitando"))
            {
                //throw new SocketException("Ataque vírus!!!");
                throw new Exception("Esperava por requisição \" Requisitando\", porém achou \" "+ requisição +" \" !");
            }
            iPConectando = IPQuemMandou;
            ConexaoP2P.tratarBroadcast();
	        //throw new Exception("B");
        }
        
        public async void receber()
        {
            /*try
            {*/
                broadcasting = (servidorBroadcast.ReceiveAsync());
                inicializarTimer(0);
            /*}
            catch (Exception ex)//ObjectDisposedException pode ser tbm
            {
		            

            }*/
        }
        public async void enviar()
        {
            try
            {
                respostaBroadcasting = (servidorBroadcast.SendAsync(Peer.msgRespBytes, Peer.msgRespBytes.Length));
                inicializarTimer(1);
            }
            catch (Exception ex)//ObjectDisposedException pode ser tbm
            {
                switch (estadoRespostaBroadcasting())
                {
                    case TaskStatus.Faulted:
                        this.finalizarRespostaBroadcasting();
                        throw new Exception("Caso aparentemente impossível!");
                        break;
                    case TaskStatus.RanToCompletion:
                        //deu tudo certo!!!
                        //não dar dispose!!
                        //esperar resposta de conexão!
                        throw new Exception("C");
                        break;
                    case TaskStatus.WaitingForChildrenToComplete:
                        this.finalizarRespostaBroadcasting();
                        throw new Exception("Comportamento inesperado da Task");
                        break;
                    case TaskStatus.Canceled:

                        this.finalizarRespostaBroadcasting();
                        throw new Exception("Task mal inicializada!");
                        break;
                    default:
                        this.finalizarRespostaBroadcasting();
                        throw new Exception("Comportamento insesperado do Timer");

                }

            }
        }
        public static TaskStatus estadoBroadcasting()
        {
            return broadcasting.Status;
        }
        public static TaskStatus estadoRespostaBroadcasting()
        {
            return respostaBroadcasting.Status;
        }
       /* public void responderPeers() {
            var infoResp = Encoding.ASCII.GetBytes("Respondendo");
            //Não sei se vou usar Send ou SendAsync

        }*/
        public void inicializarBroadcasting()
        {//                                                                 236.0.0.0
            servidorBroadcast = new UdpClient(/*new IPEndPoint(new IPAddress(0xEC000000), portaBroadcast)*/);
            //servidorBroadcast.EnableBroadcast = true;//pode enviar e/ou receber broadcast
            //servidorBroadcast.MulticastLoopback = true;
            servidorBroadcast.ExclusiveAddressUse = true;

            //uma mensagem será enviada para o dispositivo que fez um multicast

            servidorBroadcast.DontFragment = true;//não quero que fragmente os pacotes
            this.meuEnd = new IPEndPoint(IPAddress.Any, portaBroadcast);
            //servidorBroadcast.Client.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);
            servidorBroadcast.Client.Bind(meuEnd);
            servidorBroadcast.JoinMulticastGroup(endMulticast);

        }
        public static void finalizarBroadcasting()
        {
            try
            {
                if(broadcasting!=null)
                    broadcasting.Dispose();
                if (servidorBroadcast != null)
                {
                    servidorBroadcast.Close();
                    servidorBroadcast.Dispose();
                    servidorBroadcast = null;
                }
		        if(timer != null)
			        timer.Dispose();
                 }
            catch {}
        }
        public void inicializarRespostaBroadcasting()
        {
            servidorBroadcast = new UdpClient(IPConectando);
            servidorBroadcast.DontFragment = true;
            servidorBroadcast.Connect(IPConectando);
            servidorBroadcast.EnableBroadcast = false;//pode enviar e/ou receber broadcast
            //servidorBroadcast.MulticastLoopback = true;
            //uma mensagem será enviada para o dispositivo que fez um multicast

        }
        public void finalizarRespostaBroadcasting()
        {
            try
            {
                if (respostaBroadcasting != null)
                    respostaBroadcasting.Dispose();
                if (servidorBroadcast != null)
                {
                    servidorBroadcast.Close();
                    servidorBroadcast.Dispose();
                }
                if (timer != null)
                    timer.Dispose();
            }
            catch { }
        }

        //construtor para peers remotos

        public Peer(IPEndPoint ipRemoto)
        {
            if (ipRemoto == null)
                throw new ArgumentNullException("IP remoto nulo");
            this.IP = ipRemoto.Address;
        }
        //construtor para próprio peer(localhost)
        public Peer(IPAddress meuIP) {
            if (meuIP != null)
                this.IP = meuIP;
            else
                throw new ArgumentNullException("IP local nulo");

        }

    }
    
}
