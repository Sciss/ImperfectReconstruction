val t = osc.UDP.Transmitter()
t.connect()
val target: java.net.SocketAddress = "192.168.0.77" -> 57120
t.send(osc.Message("/stage", 1), target)
t.send(osc.Message("/stage", 4), target)

t.send(osc.Message("/invalid", 1), target)
