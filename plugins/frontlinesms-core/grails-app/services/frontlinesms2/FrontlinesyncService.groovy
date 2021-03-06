package frontlinesms2

class FrontlinesyncService {
	def apiProcess(connection, controller) {
		def data = controller.request.JSON
		if(connection.secret && data.secret != connection.secret) {
			return failure(controller, 'bad secret', 403)
		}

		try {
			data.payload.each { e ->
				sendMessageAndHeaders('seda:incoming-fmessages-to-store',
						new Fmessage(inbound:true,
								src:e.fromNumber,
								text:'missed call',
								date:new Date(e.callTimestamp)),
						['fconnection-id':connection.id])
			}

			controller.render text:'OK'
		} catch(Exception ex) {
			failure(controller, ex.message)
		}
	}

	private def failure(controller, message='ERROR', status=500) {
		controller.render text:message, status:status
	}
}

