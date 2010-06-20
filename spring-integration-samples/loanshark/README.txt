To change the multicast code in udps.groovy to rely on unicast instead, simply
change the MulticastSocket type to DatagramSocket and remove the next 2 lines
that deal with joining the multicast group. Then, in the configuration of the
actual adapter, set: host="127.0.0.1" and multicast="false".