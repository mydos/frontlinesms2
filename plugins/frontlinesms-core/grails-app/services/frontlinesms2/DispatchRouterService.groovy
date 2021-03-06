package frontlinesms2

import org.apache.camel.Exchange
import org.apache.camel.Header

import frontlinesms2.camel.exception.NoRouteAvailableException

/** This is a Dynamic Router */
class DispatchRouterService {
	static final String RULE_PREFIX = "fconnection-"
	def appSettingsService
	def camelContext
	def systemNotificationService

	int counter = -1
	/**
	 * Slip should return the list of ______ to forward to, or <code>null</code> if
	 * we've done with it.
	 */
	def slip(Exchange exchange,
			@Header(Exchange.SLIP_ENDPOINT) String previous,
			@Header('requested-fconnection-id') String requestedFconnectionId) {
		def log = { println "DispatchRouterService.slip() : $it" }
		log "ENTRY"
		log "Routing exchange $exchange with previous endpoint $previous and target fconnection $requestedFconnectionId"
		log "x.in=$exchange?.in"
		log "x.in.headers=$exchange?.in?.headers"

		if(previous) {
			// We only want to pass this message to a single endpoint, so if there
			// is a previous one set, we should exit the slip.
			log "Exchange has previous endpoint from this slip.  Returning null."
			return null
		} else if(requestedFconnectionId) {
			log "Target is set, so forwarding exchange to fconnection $requestedFconnectionId"
			return "seda:out-$requestedFconnectionId"
		} else {
			def routeId
			log "appSettingsService.['routing.use'] is ${appSettingsService.get('routing.use')}"

			if(appSettingsService.get('routing.use')) {
				def fconnectionRoutingList = appSettingsService.get('routing.use').split(/\s*,\s*/)
				fconnectionRoutingList = fconnectionRoutingList.collect { route ->
					route.startsWith(RULE_PREFIX)? route.substring(RULE_PREFIX.size()): route
				}
				log "fconnectionRoutingList::: $fconnectionRoutingList"
				for(route in fconnectionRoutingList) {
					if(route == 'uselastreceiver') {
						def fconnection = getLastReceiverConnection(exchange)
						route = fconnection?.id
						routeId = fconnection ? getCamelRouteId(fconnection) : null
					} else {
						routeId = getCamelRouteId(Fconnection.get(route))
					}
					log "Route Id selected: $routeId"
					if(routeId) {
						if(exchange.in.body instanceof Dispatch) {
							def dispatch = exchange.in.body
							log " dispatch.fconnectionId:::: $route"
							dispatch.fconnectionId = route as Long
							dispatch.save(failOnError:true, flush:true)
						}
						break
					}
				}
			}

			if(routeId) {
				log "Sending with route: $routeId"
				def fconnectionId = (routeId =~ /.*-(\d+)$/)[0][1]
				def queueName = "seda:out-$fconnectionId"
				log "Routing to $queueName"
				return queueName
			} else { log "## Not sending message at all ##" }

			throw new NoRouteAvailableException()
		}
	}
	
	def getRouteIdByRoundRobin() {
		def allOutRoutes = camelContext.routes.findAll { it.id.startsWith('out-') }
		if(allOutRoutes.size > 0) {
			// check for internet routes and prioritise them over modems
			def filteredRouteList = allOutRoutes.findAll { it.id.contains('-internet-') }
			if(!filteredRouteList) filteredRouteList = allOutRoutes.findAll { it.id.contains('-modem-') }
			if(!filteredRouteList) filteredRouteList = allOutRoutes
			
			println "DispatchRouterService.getRouteIdByRoundRobin() : Routes available: ${filteredRouteList*.id}"
			println "DispatchRouterService.getRouteIdByRoundRobin() : Counter has counted up to $counter"
			return filteredRouteList[++counter % filteredRouteList.size]?.id
		}
	}

	def handleCompleted(Exchange x) {
		println "DispatchRouterService.handleCompleted() : ENTRY"
		def connection = Fconnection.get(x.in.getHeader('fconnection-id'))
		connection?.updateDispatch(x)
		println "DispatchRouterService.handleCompleted() : EXIT"
	}

	def handleFailed(Exchange x) {
		println "DispatchRouterService.handleFailed() : ENTRY"
		updateDispatch(x, DispatchStatus.FAILED)
		println "DispatchRouterService.handleFailed() : EXIT"
	}

	def handleNoRoutes(Exchange x) {
		println "DispatchRouterService.handleNoRoutes() : NoRouteAvailableException handling..."
		systemNotificationService.create(code:"routing.notification.no-available-route")
		x.out.body = x.in.body
		x.out.headers = x.in.headers
		println "DispatchRouterService.handleNoRoutes() : EXIT"
	}

	private Dispatch updateDispatch(Exchange x, s) {
		def id = x.in.getHeader('frontlinesms.dispatch.id')
		Dispatch d
		if(x.in.body instanceof Dispatch) {
			d = x.in.body
			d.refresh()
		} else {
			d = Dispatch.get(id)
		}
		
		println "DispatchRouterService.updateDispatch() : dispatch=$d" 
		
		if(d) {
			d.status = s
			if(s == DispatchStatus.SENT) d.dateSent = new Date()

			try {
				d.save(failOnError:true, flush:true)
			} catch(Exception ex) {
				log.error("Could not save dispatch $d with message $d.message", ex)
			}
		} else log.info("No dispatch found for id: $id")
	}

	private getLastReceiverConnection(exchange) {
		def log = { println "DispatchRouterService.slip() : $it" }
		log "Dispatch is ${exchange.in.getBody()}"
		def d = exchange.in.getBody()
		log "dispatch to send # $d ### d.dst # $d?.dst"
		def latestReceivedMessage = Fmessage.findBySrc(d.dst, [sort: 'dateCreated', order:'desc'])
		log "## latestReceivedMessage ## is $latestReceivedMessage"
		latestReceivedMessage?.receivedOn
	}

	private getCamelRouteId(connection) {
		if(!connection) return null
		println "## Sending message with Connection with $connection ##"
		def allOutRoutes = camelContext.routes.findAll { it.id.startsWith('out-') }
		def routeToTake = allOutRoutes.find{ it.id.endsWith("-${connection.id}") }
		println "Chosen Route ## $routeToTake"
		routeToTake? routeToTake.id: null
	}
}

