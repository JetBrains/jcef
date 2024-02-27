module org.apache.thrift {
	requires java.base;
	exports org.apache.thrift;
	exports org.apache.thrift.server;
	exports org.apache.thrift.protocol;
	exports org.apache.thrift.scheme;
	exports org.apache.thrift.meta_data;
	exports org.apache.thrift.transport;

	requires org.slf4j;
}
