import boto3
import time

# comandi per terminale prima di avviare lo script

AMI_SERVER = "XXXXXXXXXXXXXXX"
#AMI_ZK = "ami-00a4ea85411394cdb" non serve
KEY_PEM = "sabdKey"
INSTANCE_TYPE_SMALL = "t2.small"
INSTANCE_TYPE_MICRO = "t2.micro"
INSTANCE_TYPE_MEDIUM = "t2.medium"
INSTANCE_TYPE_LARGE = "t2.large"
SERVICE_LAUNCH_CONFIGURATION = "service_launch_configuration"
SERVER_LAUNCH_CONFIGURATION = "server_launch_configuration"
AWS_ACCESS_KEY_ID = "XXXXXXXXXXXXXXX" 
AWS_SECRET_ACCESS_KEY = "XXXXXXXXXXXXXXX" 


MIN_KAFKA_CLUSTER_SIZE = 2
DESIRED_KAFKA_CLUSTER_SIZE = 2
MAX_KAFKA_CLUSTER_SIZE = 2


NUM_AVAILABILITY_ZONE = 3


#IPZOOKEEPER -> 10.0.0.11 o 10.0.1.11 o 10.0.2.11


def create_eni(ip, sg_id, i):
    return subnets[i].create_network_interface(
        Groups=[
            sg_id,
        ],
        PrivateIpAddress=ip
    )

def create_kafka_user_data(alloc_id):
    return """#!/bin/bash
    sleep 60
    sudo -s
    apt-get update
    echo "127.0.0.1" $HOSTNAME >> /etc/hosts
    apt-get install -y openjdk-8-jdk python2.7 python-pip
    pip install awscli
    aws configure set region eu-central-1
    aws configure set aws_access_key_id """+AWS_ACCESS_KEY_ID+"""
    aws configure set aws_secret_access_key """+AWS_SECRET_ACCESS_KEY+"""
    LOCALIP=$(ifconfig eth0 | grep "inet addr" | cut -d ':' -f 2 | cut -d ' ' -f 1)
    query=$(aws ec2 describe-addresses --allocation-ids """ + alloc_id + """)
    query=$(echo "$query" | jq ".Addresses[0]")
    INSTANCEID=$(curl -s http://169.254.169.254/latest/meta-data/instance-id)
    aws ec2 create-tags --resources $INSTANCEID --tags 'Key="Name",Value="Kafka"'
    if echo "$query" | jq -e 'has("AssociationId")' > /dev/null; then echo "address already associated"; else aws ec2 associate-address --allocation-id \"""" + alloc_id + """ \"instance-id "$INSTANCEID" --no-allow-reassociation --private-ip-address "$LOCALIP" --region eu-central-1; fi
    sleep 15
    echo "nameserver 8.8.8.8" >> /etc/resolv.conf
    wget https://www-eu.apache.org/dist/kafka/2.2.0/kafka_2.11-2.2.0.tgz
    mkdir /opt/kafka
    tar xvzf kafka_2.11-2.2.0.tgz -C /opt/kafka
    rm kafka_2.11-2.2.0.tgz
    cd /opt/kafka/kafka_2.11-2.2.0/config
    sed -i "/zookeeper.connect=localhost:2181/c\zookeeper.connect=10.0.0.11:2181,10.0.1.11:2181,10.0.2.11:2181" server.properties
    MYIP=$(dig +short myip.opendns.com @resolver1.opendns.com)
    LOCALID=${LOCALIP:5}
    LOCALID=$(echo $LOCALID | tr -d .)
    sed -i "/#advertised.listeners=PLAINTEXT:\/\/your.host.name:9092/c\\advertised.listeners=PLAINTEXT:\/\/$MYIP:9092" server.properties
    sed -i "/#listeners=PLAINTEXT:\/\/:9092/c\listeners=PLAINTEXT:\/\/0.0.0.0:9092" server.properties
    sed -i "/broker.id=0/c\\broker.id=$LOCALID" server.properties
    echo -e "\nreserved.broker.max.id=10000" >> server.properties
    #mount volume
    mkfs.ext4 /dev/xvdb
    mkdir /broker
    mount /dev/xvdb /broker
    sleep 10
    rm -rf /broker
    sed -i "/log.dirs=/tmp/kafka-logs/c\log.dirs=/broker" server.properties
    cd /opt/kafka/kafka_2.11-1.1.1/
    bin/kafka-server-start.sh config/server.properties
    sleep 10
    bin/kafka-server-start.sh config/server.properties
    """




def create_zk_user_data(eni_id, ipaddr):
    hostaddr = "ip-" + ipaddr.replace(".", "-")
    return """#!/bin/bash
    sleep 30
    sudo -s
    apt-get update
    export MYIP=$(curl -s http://169.254.169.254/latest/meta-data/local-ipv4)
    export MYIPaddr=${MYIP//./-}
    echo "127.0.0.1 """ + hostaddr + """" >> /etc/hosts
    echo "127.0.0.1" $HOSTNAME >> /etc/hosts
    echo "nameserver 8.8.8.8" >> /etc/resolv.conf
    apt-get install -y openjdk-8-jdk zookeeperd python2.7 python-pip jq
    pip install awscli
    aws configure set region eu-central-1
    aws configure set aws_access_key_id """+AWS_ACCESS_KEY_ID+"""
    aws configure set aws_secret_access_key """+AWS_SECRET_ACCESS_KEY+"""
    export INSTANCEID=$(curl -s http://169.254.169.254/latest/meta-data/instance-id)
    aws ec2 create-tags --resources $INSTANCEID --tags 'Key="Name",Value="Zookeeper"'
    aws ec2 attach-network-interface --region eu-central-1 --instance-id $INSTANCEID --device-index 1 --network-interface-id """ + eni_id + """
    sleep 15
    sed -i "/#server.1=zookeeper1:2888:3888/c\server.1=10.0.0.11:2888:3888" /etc/zookeeper/conf/zoo.cfg
    sed -i "/#server.2=zookeeper2:2888:3888/c\server.2=10.0.1.11:2888:3888" /etc/zookeeper/conf/zoo.cfg
    sed -i "/#server.3=zookeeper3:2888:3888/c\server.3=10.0.2.11:2888:3888" /etc/zookeeper/conf/zoo.cfg
    echo "maxClientCnxns=1000" >> /etc/zookeeper/conf/zoo.cfg
    sleep 15
    ifconfig eth1 up
    ifconfig eth1 """ + ipaddr + """ netmask 255.255.255.0
    GATEWAY_IP=$( /sbin/ip route | awk '/default/ { print $3 }' )
    echo -e "auto eth1\niface eth1 inet dhcp\n  post-up ip route add default via $GATEWAY_IP dev eth1 tab 2\n  post-up ip rule add from """ + ipaddr + """/32 tab 2 priority 600" > /etc/network/interfaces.d/eth1.cfg
    service networking restart
    sleep 10
    LOCALIP=$(ifconfig eth1 | grep "inet addr" | cut -d ':' -f 2 | cut -d ' ' -f 1)
    LOCALID=$(echo "$LOCALIP" | cut -c6 )
    echo $((LOCALID + 1)) > /etc/zookeeper/conf/myid
    service zookeeper stop
    service zookeeper start
    """


#########  START SCRIPT ############

ec2 = boto3.resource('ec2')
ec2_client = boto3.client('ec2')
vpc = ec2.create_vpc(CidrBlock='10.0.0.0/16')
# we can assign a name to vpc, or any resource, by using tag
vpc.create_tags(Tags=[{"Key": "Name", "Value": "vpc"}])
vpc.wait_until_available()
print(vpc.id)

# create subnets
subnets = []
for i in range(NUM_AVAILABILITY_ZONE):
    subnets.append(ec2.create_subnet(CidrBlock='10.0.' + str(i) + '.0/24', VpcId=vpc.id, AvailabilityZone='eu-central-1' + chr(97 + i)))
    print(subnets[i].id)

# Create EIPs
eips = []
for i in range(NUM_AVAILABILITY_ZONE):
    eips.append(ec2_client.allocate_address(
        Domain='vpc'
    ))



# Create sec group kafka
sec_group_kafka = ec2.create_security_group(
    GroupName='kafka', Description='kafka sec group', VpcId=vpc.id)
sec_group_kafka.authorize_ingress(
      IpPermissions=[{'IpProtocol': 'icmp', 'FromPort': -1, 'ToPort': -1, 'IpRanges': [{'CidrIp': '0.0.0.0/0'}]},
                     {'IpProtocol': 'tcp', 'FromPort': 22, 'ToPort': 22, 'IpRanges': [{'CidrIp': '0.0.0.0/0'}]},
                     {'IpProtocol': 'tcp', 'FromPort': 9092, 'ToPort': 9092, 'IpRanges': [{'CidrIp': '0.0.0.0/0'}]}]
)
print(sec_group_kafka.id)

# Create sec group zookeeper
sec_group_zookeeper = ec2.create_security_group(
    GroupName='zookeeper', Description='zookeeper', VpcId=vpc.id)
sec_group_zookeeper.authorize_ingress(
      IpPermissions=[{'IpProtocol': 'icmp', 'FromPort': -1, 'ToPort': -1, 'IpRanges': [{'CidrIp': '0.0.0.0/0'}]},
                     {'IpProtocol': 'tcp', 'FromPort': 22, 'ToPort': 22, 'IpRanges': [{'CidrIp': '0.0.0.0/0'}]},
                     {'IpProtocol': 'tcp', 'FromPort': 2181, 'ToPort': 2181, 'IpRanges': [{'CidrIp': '0.0.0.0/0'}]},
                     {'IpProtocol': 'tcp', 'FromPort': 2888, 'ToPort': 2888, 'IpRanges': [{'CidrIp': '0.0.0.0/0'}]},
                     {'IpProtocol': '-1', 'FromPort': -1, 'ToPort': -1, 'IpRanges': [{'CidrIp': '0.0.0.0/0'}]},
                     {'IpProtocol': 'tcp', 'FromPort': 3888, 'ToPort': 3888, 'IpRanges': [{'CidrIp': '0.0.0.0/0'}]}]
)
print(sec_group_zookeeper.id)



# Create Elastic Network Interfaces for zookeeper instances
eni_zk = [create_eni('10.0.0.11', sec_group_zookeeper.id, 0), create_eni('10.0.1.11', sec_group_zookeeper.id, 1),
          create_eni('10.0.2.11', sec_group_zookeeper.id, 2)]


# create then attach internet gateway
ig = ec2.create_internet_gateway()
vpc.attach_internet_gateway(InternetGatewayId=ig.id)
print(ig.id)

# create a route table and a public route
route_table = vpc.create_route_table()
route = route_table.create_route(
    DestinationCidrBlock='0.0.0.0/0',
    GatewayId=ig.id
)

print(route_table.id)


# associate the route table with the subnets
for i in range(NUM_AVAILABILITY_ZONE):
    route_table.associate_with_subnet(SubnetId=subnets[i].id)



autoscaling_group_client = boto3.client('autoscaling')



# create launch config for zookeeper
user_data_zookeeper = [create_zk_user_data(eni_zk[0].id, '10.0.0.11'), create_zk_user_data(eni_zk[1].id, '10.0.1.11'),
                       create_zk_user_data(eni_zk[2].id, '10.0.2.11')]
# Create zookeeper launch configuration
for i in range(0, len(user_data_zookeeper)):
    autoscaling_group_client.create_launch_configuration(
         LaunchConfigurationName='launch_configuration_zookeeper_' + str(i),
         ImageId=AMI_SERVER,
         UserData=user_data_zookeeper[i],
         SecurityGroups=[
                         sec_group_zookeeper.id
                         ],
         InstanceType=INSTANCE_TYPE_SMALL,
         AssociatePublicIpAddress=True,
         KeyName=KEY_PEM
     )
print('zk launch config')



# Create kafka launch configuration
for i in range(NUM_AVAILABILITY_ZONE):
    user_data_kafka = create_kafka_user_data(eips[i]["AllocationId"])
    autoscaling_group_client.create_launch_configuration(
         LaunchConfigurationName='launch_configuration_kafka_' + str(i),
         ImageId=AMI_SERVER,
         UserData=user_data_kafka,
         SecurityGroups=[
             sec_group_kafka.id
         ],
         InstanceType=INSTANCE_TYPE_SMALL,
         AssociatePublicIpAddress=True,
         KeyName=KEY_PEM,
         BlockDeviceMappings=[{
          'DeviceName': '/dev/sdb',
          'Ebs': {
              'VolumeSize': 20
              }
          }]
     )
print('kafka launch config')


# Create zookeeper autoscaling group configuration
for i in range(NUM_AVAILABILITY_ZONE):
    autoscaling_group_client.create_auto_scaling_group(
       VPCZoneIdentifier=subnets[i].id,
       AutoScalingGroupName='autoscaling-zookeeper-' + str(i),
       LaunchConfigurationName='launch_configuration_zookeeper_' + str(i),
       MinSize=1,
       MaxSize=1
    )


# Create kafka autoscaling group configuration
for i in range(NUM_AVAILABILITY_ZONE):
    autoscaling_group_client.create_auto_scaling_group(
       VPCZoneIdentifier=subnets[i].id,
       AutoScalingGroupName='autoscaling-kafka-' + chr(97 + i),
       LaunchConfigurationName='launch_configuration_kafka_' + str(i),
       MinSize=MIN_KAFKA_CLUSTER_SIZE,
       MaxSize=MAX_KAFKA_CLUSTER_SIZE,
       DesiredCapacity=DESIRED_KAFKA_CLUSTER_SIZE
   )
