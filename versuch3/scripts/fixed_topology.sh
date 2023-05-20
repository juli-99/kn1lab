#close open socket to prevent mininet error
sudo fuser -k 6653/tcp
sudo python3 ./fixed_topology.py
