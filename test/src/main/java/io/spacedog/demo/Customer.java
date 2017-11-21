package io.spacedog.demo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.DataObject;
import io.spacedog.model.DataObjectAbstract;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class Customer {

	public String status;
	public String firstname;
	public String lastname;
	public String phone;
	public String photo;

	public static class CustomerDataObject extends DataObjectAbstract<Customer> {

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
		public DataObject<Customer> source(Customer source) {
			this.source = source;
			return this;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Results {

		public long total;
		public List<CustomerDataObject> results;
		public ObjectNode aggregations;
	}

}
