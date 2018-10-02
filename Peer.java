public class Peer{
	private MulticastSocket emissor;
	private Timer temporizador;
	private static final int portaAppBroadcasting = 1729;
	private static final int portaDesktopBroadcasting = 1639;
	private static final String msgReq = "Requisitando";
	private static final String msgResp = "Respondendo";
	private static final String endGrupo = "236.0.0.0";
	private MulticastSocket mcastEnviar;
	private MulticastSocket mcastReceber;
	private InetAddress meuIP;
	private InetAddress endBroadcast;
	private DatagramPacket pacoteEnviar;
	private DatagramPacket pacoteReceber;
	private static final byte[] msgReqBytes = msgReq.getBytes(Charset.forName("UTF-8"));
	private static final int ttlEnviar = 1000;//1 seg
	private static final int ttlReceber = 10000;//10 seg
	private int ttlPadrao;
	public ReceberUDPThread escuta;
	public EnviarUDPThread broadcasting;
	private ChecadorStatus checador;
	private InetAddress iPConectando;
	public InetAddress getIPConectando(){
		return this.iPConectando;
	}
	public InetAddress getIP(){
		return this.meuIP;
	}
	/*
	alguns m�todos necess�rios
	inicializarTimer
	inicializarBroadcasting
	finalizarBroadcasting
	enviar
	estadoBroadcasting
	construtor
	*/
	/*
https://docs.oracle.com/javase/9/docs/api/java/lang/doc-files/threadPrimitiveDeprecation.html*/
	public Peer(InetAddress meuIP)throws Exception{
		if(meuIP != null)
			this.meuIP = meuIP;
		else
			throw new Exception("IP local nulo");

		this.endBroadcast = InetAddress.getByName(endGrupo);
		//constr�i o socket com os dados de quem envia
		//nesse caso, ele envia por uma porta aleat�ria
		//pois n�o pus nada no par�metro
		this.mcastEnviar = new MulticastSocket();
		
		this.mcastReceber = new MulticastSocket(Peer.portaAppBroadcasting);
		
		//monta o pacote com os dados de quem ir� receber
		this.pacoteEnviar = new DatagramPacket(
		Peer.msgReqBytes,Peer.msgReqBytes.length, endBroadcast, Peer.portaDesktopBroadcasting);
		byte[] buffer = new byte[1024];
		this.pacoteReceber = new DatagramPacket(buffer, buffer.length);
		
	}
	public void inicializarBroadcasting(){
		//endere�o do grupo multicast(quem espera a requisi��o)
		//escolher um endere�o aleat�rio de 225.0.0.0 a 238.255.255.255
		/*Endere�os multicast de link-local come�am com 224.0.0
		(ou seja, os endere�os de 224.0.0.0 a 224.0.0.255)
		e est�o reservados para protocolos de roteamento
		e outras atividades de baixo n�vel,
		como descoberta de gateway
		e relat�rios de participa��o em grupo*/
		//Roteadores multicast nunca encaminham
		//datagramas com destinos nesse intervalo.
		this.mcastEnviar.joinGroup(endBroadcast);
		this.ttlPadrao= mcastEnviar.getTimeToLive();
		this.mcastSocket.setTimeToLive(Peer.ttlEnviar);
		this.broadcasting = new EnviarUDPThread(
		"broadcasting",pacoteEnviar, mcastEnviar);

		this.temporizador = new Timer("broadcasting", false);
		//n�o � uma task daemon, por isso � false
		this.checador = new ChecadorStatus(this.broadcasting);
		
		
	}
	public void enviar(){
		/*//por enquanto vou deixar s�ncrono
		mcastEnviar.send(pacoteBroadcast);*/
		this.broadcasting.start();
		this.temporizador.schedule(this.checador, 0, 1000);
		//      		     			inicial delay periodo
	}
	public void depoisEnviar(){
	//aqui temos certeza que a thread de enviar acabou
		Thread.Status comoAcabou = this.checador.getStatus();
		this.mcastEnviar.setTimeToLive(this.ttlPadrao);
		if(comoAcabou != Thread.State.TERMINATED){
			
			this.finalizarBroadcasting();
			throw new Exception("Conex�o com o dispositivo mal estabelecida! Verifique se est�o na mesma rede e tente novamente!");
		}
		else{
			
			//deu certo!
			ConexaoP2P.depoisEnviar();
		}		
	}
	public void finalizarBroadcasting(){
		try{
			if(this.mcastEnviar != null){
				this.mcastEnviar.leaveGroup(endBroadcast);
				this.mcastEnviar.close();
				this.mcastEnviar.disconnect();
				this.mcastEnviar = null;
			}
			
			if(this.temporizador!=null){
				this.temporizador.purge();
				this.temporizador.cancel();
				this.temporizador = null;

			}
			this.checador=null;
			this.broadcasting=null;
		}
		catch(Exception ex){}
	}
	public void inicializarEscuta(){
		//endere�o do grupo multicast(quem espera a requisi��o)
		//escolher um endere�o aleat�rio de 225.0.0.0 a 238.255.255.255
		/*Endere�os multicast de link-local come�am com 224.0.0
		(ou seja, os endere�os de 224.0.0.0 a 224.0.0.255)
		e est�o reservados para protocolos de roteamento
		e outras atividades de baixo n�vel,
		como descoberta de gateway
		e relat�rios de participa��o em grupo*/
		//Roteadores multicast nunca encaminham
		//datagramas com destinos nesse intervalo.
		//monta o pacote com os dados de quem ir� receber
		//constr�i o socket com os dados de quem envia
		//nesse caso, ele envia por uma porta aleat�ria
		//pois n�o pus nada no par�metro
		mcastReceber.joinGroup(endBroadcast);
		this.ttlPadrao= mcastReceber.getTimeToLive();
		mcastReceber.setTimeToLive(Peer.ttlReceber);
		this.escuta = new ReceberUDPThread(
		"escutaUDP",pacoteReceber, mcastReceber);

		this.temporizador = new Timer("escutaUDP", false);
		this.checador = null;
		this.checador = new ChecadorStatus(this.escuta);
		/*
		int ttl = mcastSocket.getTimeToLive(); mcastSocket.setTimeToLive(newttl); mcastSocket.send(p); mcastSocket.setTimeToLive(ttl);
		*/
	}
	public void receber(){
		//c�digo rodando assincronamente
		this.escuta.start();
		this.temporizador.schedule(this.checador, 0, 1000);
	}
	public void depoisEscuta(){
		Thread.Status comoAcabou = this.checador.getStatus();
		this.mcastEnviar.setTimeToLive(this.ttlPadrao);
		if(comoAcabou != Thread.State.TERMINATED){
			
			this.finalizarEscuta();
			throw new Exception("Conex�o com o dispositivo mal estabelecida! Verifique se est�o na mesma rede e tente novamente!");
		}
		else{
			//deu certo!
			byte[] buffer = this.escuta.getDados();
			InetAddress ipQuemEnviou = this.escuta.getIPQuemEnviou();
			byte[] requerido = this.Peer.msgResp.getBytes(Charset.forName("UTF-8"));
			boolean iguais = true;
			if(buffer.length == requerido.length){
				for(int i=0;i<buffer.length;i++){
					if(buffer[i] != requerido[i])
						iguais = false;
				}
			}
			if(buffer.length!=requerido.length || !iguais){
				//ataque v�rus!!!
				throw new Exception("Ataque v�rus!!");
			}
			else{
				this.iPConectando = ipQuemEnviou;

				ConexaoP2P.depoisEscuta();
			}

	
		}
	}
	public void finalizarEscuta(){
		try{
			if(this.mcastReceber != null){
				this.mcastReceber.leaveGroup(endBroadcast);
				this.mcastReceber.close();
				this.mcastReceber.disconnect();
				this.mcastReceber = null;
			}
			
			if(this.temporizador!=null){
				this.temporizador.purge();
				this.temporizador.cancel();
				this.temporizador = null;

			}
			this.checador=null;
			this.escuta=null;
		}
		catch(Exception ex){}
	}
/*MulticastSocket ms = new MulticastSocket(2300);
InetAddress group = InetAddress.getByName("224.2.2.2");
ms.joinGroup(group);
byte[] buffer = new byte[8192];
DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
ms.receive(dp);
ms.leaveGroup(group);
ms.close(););*/


}