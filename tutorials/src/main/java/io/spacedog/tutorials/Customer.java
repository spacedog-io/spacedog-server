package io.spacedog.tutorials;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.data.DataObjectBase;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.data.DataWrapAbstract;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class Customer extends DataObjectBase {

	public String status;
	public String firstname;
	public String lastname;
	public String phone;
	public String photo;

	public static class Wrap extends DataWrapAbstract<Customer> {

		private Customer source;

		@Override
		public Class<Customer> sourceClass() {
			return Customer.class;
		}

		@Override
		public Customer source() {
			return source;
		}

		@Override
		public DataWrap<Customer> source(Customer source) {
			this.source = source;
			return this;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Results {

		public long total;
		public List<Wrap> results;
		public ObjectNode aggregations;
	}

}
