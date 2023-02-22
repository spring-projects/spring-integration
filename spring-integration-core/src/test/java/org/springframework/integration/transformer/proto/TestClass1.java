// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: ProtoTest.proto

package org.springframework.integration.transformer.proto;

/**
 * Protobuf type {@code tutorial.TestClass1}
 */
public  final class TestClass1 extends
		com.google.protobuf.GeneratedMessageV3 implements
		// @@protoc_insertion_point(message_implements:tutorial.TestClass1)
		TestClass1OrBuilder {
private static final long serialVersionUID = 0L;
	// Use TestClass1.newBuilder() to construct.
	private TestClass1(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
		super(builder);
	}
	private TestClass1() {
		bar_ = "";
	}

	@java.lang.Override
	@SuppressWarnings({"unused"})
	protected java.lang.Object newInstance(
			UnusedPrivateParameter unused) {
		return new TestClass1();
	}

	@java.lang.Override
	public final com.google.protobuf.UnknownFieldSet
	getUnknownFields() {
		return this.unknownFields;
	}
	private TestClass1(
			com.google.protobuf.CodedInputStream input,
			com.google.protobuf.ExtensionRegistryLite extensionRegistry)
			throws com.google.protobuf.InvalidProtocolBufferException {
		this();
		if (extensionRegistry == null) {
			throw new java.lang.NullPointerException();
		}
		int mutable_bitField0_ = 0;
		com.google.protobuf.UnknownFieldSet.Builder unknownFields =
				com.google.protobuf.UnknownFieldSet.newBuilder();
		try {
			boolean done = false;
			while (!done) {
				int tag = input.readTag();
				switch (tag) {
					case 0:
						done = true;
						break;
					case 10: {
						com.google.protobuf.ByteString bs = input.readBytes();
						bitField0_ |= 0x00000001;
						bar_ = bs;
						break;
					}
					case 16: {
						bitField0_ |= 0x00000002;
						qux_ = input.readInt32();
						break;
					}
					default: {
						if (!parseUnknownField(
								input, unknownFields, extensionRegistry, tag)) {
							done = true;
						}
						break;
					}
				}
			}
		} catch (com.google.protobuf.InvalidProtocolBufferException e) {
			throw e.setUnfinishedMessage(this);
		} catch (java.io.IOException e) {
			throw new com.google.protobuf.InvalidProtocolBufferException(
					e).setUnfinishedMessage(this);
		} finally {
			this.unknownFields = unknownFields.build();
			makeExtensionsImmutable();
		}
	}
	public static final com.google.protobuf.Descriptors.Descriptor
			getDescriptor() {
		return org.springframework.integration.transformer.proto.TestProtos.internal_static_tutorial_TestClass1_descriptor;
	}

	@java.lang.Override
	protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
			internalGetFieldAccessorTable() {
		return org.springframework.integration.transformer.proto.TestProtos.internal_static_tutorial_TestClass1_fieldAccessorTable
				.ensureFieldAccessorsInitialized(
						org.springframework.integration.transformer.proto.TestClass1.class, org.springframework.integration.transformer.proto.TestClass1.Builder.class);
	}

	private int bitField0_;
	public static final int BAR_FIELD_NUMBER = 1;
	private volatile java.lang.Object bar_;
	/**
	 * <code>optional string bar = 1;</code>
	 * @return Whether the bar field is set.
	 */
	public boolean hasBar() {
		return ((bitField0_ & 0x00000001) != 0);
	}
	/**
	 * <code>optional string bar = 1;</code>
	 * @return The bar.
	 */
	public java.lang.String getBar() {
		java.lang.Object ref = bar_;
		if (ref instanceof java.lang.String) {
			return (java.lang.String) ref;
		} else {
			com.google.protobuf.ByteString bs =
					(com.google.protobuf.ByteString) ref;
			java.lang.String s = bs.toStringUtf8();
			if (bs.isValidUtf8()) {
				bar_ = s;
			}
			return s;
		}
	}
	/**
	 * <code>optional string bar = 1;</code>
	 * @return The bytes for bar.
	 */
	public com.google.protobuf.ByteString
			getBarBytes() {
		java.lang.Object ref = bar_;
		if (ref instanceof java.lang.String) {
			com.google.protobuf.ByteString b =
					com.google.protobuf.ByteString.copyFromUtf8(
							(java.lang.String) ref);
			bar_ = b;
			return b;
		} else {
			return (com.google.protobuf.ByteString) ref;
		}
	}

	public static final int QUX_FIELD_NUMBER = 2;
	private int qux_;
	/**
	 * <code>optional int32 qux = 2;</code>
	 * @return Whether the qux field is set.
	 */
	public boolean hasQux() {
		return ((bitField0_ & 0x00000002) != 0);
	}
	/**
	 * <code>optional int32 qux = 2;</code>
	 * @return The qux.
	 */
	public int getQux() {
		return qux_;
	}

	private byte memoizedIsInitialized = -1;
	@java.lang.Override
	public final boolean isInitialized() {
		byte isInitialized = memoizedIsInitialized;
		if (isInitialized == 1) return true;
		if (isInitialized == 0) return false;

		memoizedIsInitialized = 1;
		return true;
	}

	@java.lang.Override
	public void writeTo(com.google.protobuf.CodedOutputStream output)
											throws java.io.IOException {
		if (((bitField0_ & 0x00000001) != 0)) {
			com.google.protobuf.GeneratedMessageV3.writeString(output, 1, bar_);
		}
		if (((bitField0_ & 0x00000002) != 0)) {
			output.writeInt32(2, qux_);
		}
		unknownFields.writeTo(output);
	}

	@java.lang.Override
	public int getSerializedSize() {
		int size = memoizedSize;
		if (size != -1) return size;

		size = 0;
		if (((bitField0_ & 0x00000001) != 0)) {
			size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, bar_);
		}
		if (((bitField0_ & 0x00000002) != 0)) {
			size += com.google.protobuf.CodedOutputStream
				.computeInt32Size(2, qux_);
		}
		size += unknownFields.getSerializedSize();
		memoizedSize = size;
		return size;
	}

	@java.lang.Override
	public boolean equals(final java.lang.Object obj) {
		if (obj == this) {
		 return true;
		}
		if (!(obj instanceof org.springframework.integration.transformer.proto.TestClass1)) {
			return super.equals(obj);
		}
		org.springframework.integration.transformer.proto.TestClass1 other = (org.springframework.integration.transformer.proto.TestClass1) obj;

		if (hasBar() != other.hasBar()) return false;
		if (hasBar()) {
			if (!getBar()
					.equals(other.getBar())) return false;
		}
		if (hasQux() != other.hasQux()) return false;
		if (hasQux()) {
			if (getQux()
					!= other.getQux()) return false;
		}
		if (!unknownFields.equals(other.unknownFields)) return false;
		return true;
	}

	@java.lang.Override
	public int hashCode() {
		if (memoizedHashCode != 0) {
			return memoizedHashCode;
		}
		int hash = 41;
		hash = (19 * hash) + getDescriptor().hashCode();
		if (hasBar()) {
			hash = (37 * hash) + BAR_FIELD_NUMBER;
			hash = (53 * hash) + getBar().hashCode();
		}
		if (hasQux()) {
			hash = (37 * hash) + QUX_FIELD_NUMBER;
			hash = (53 * hash) + getQux();
		}
		hash = (29 * hash) + unknownFields.hashCode();
		memoizedHashCode = hash;
		return hash;
	}

	public static org.springframework.integration.transformer.proto.TestClass1 parseFrom(
			java.nio.ByteBuffer data)
			throws com.google.protobuf.InvalidProtocolBufferException {
		return PARSER.parseFrom(data);
	}
	public static org.springframework.integration.transformer.proto.TestClass1 parseFrom(
			java.nio.ByteBuffer data,
			com.google.protobuf.ExtensionRegistryLite extensionRegistry)
			throws com.google.protobuf.InvalidProtocolBufferException {
		return PARSER.parseFrom(data, extensionRegistry);
	}
	public static org.springframework.integration.transformer.proto.TestClass1 parseFrom(
			com.google.protobuf.ByteString data)
			throws com.google.protobuf.InvalidProtocolBufferException {
		return PARSER.parseFrom(data);
	}
	public static org.springframework.integration.transformer.proto.TestClass1 parseFrom(
			com.google.protobuf.ByteString data,
			com.google.protobuf.ExtensionRegistryLite extensionRegistry)
			throws com.google.protobuf.InvalidProtocolBufferException {
		return PARSER.parseFrom(data, extensionRegistry);
	}
	public static org.springframework.integration.transformer.proto.TestClass1 parseFrom(byte[] data)
			throws com.google.protobuf.InvalidProtocolBufferException {
		return PARSER.parseFrom(data);
	}
	public static org.springframework.integration.transformer.proto.TestClass1 parseFrom(
			byte[] data,
			com.google.protobuf.ExtensionRegistryLite extensionRegistry)
			throws com.google.protobuf.InvalidProtocolBufferException {
		return PARSER.parseFrom(data, extensionRegistry);
	}
	public static org.springframework.integration.transformer.proto.TestClass1 parseFrom(java.io.InputStream input)
			throws java.io.IOException {
		return com.google.protobuf.GeneratedMessageV3
				.parseWithIOException(PARSER, input);
	}
	public static org.springframework.integration.transformer.proto.TestClass1 parseFrom(
			java.io.InputStream input,
			com.google.protobuf.ExtensionRegistryLite extensionRegistry)
			throws java.io.IOException {
		return com.google.protobuf.GeneratedMessageV3
				.parseWithIOException(PARSER, input, extensionRegistry);
	}
	public static org.springframework.integration.transformer.proto.TestClass1 parseDelimitedFrom(java.io.InputStream input)
			throws java.io.IOException {
		return com.google.protobuf.GeneratedMessageV3
				.parseDelimitedWithIOException(PARSER, input);
	}
	public static org.springframework.integration.transformer.proto.TestClass1 parseDelimitedFrom(
			java.io.InputStream input,
			com.google.protobuf.ExtensionRegistryLite extensionRegistry)
			throws java.io.IOException {
		return com.google.protobuf.GeneratedMessageV3
				.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
	}
	public static org.springframework.integration.transformer.proto.TestClass1 parseFrom(
			com.google.protobuf.CodedInputStream input)
			throws java.io.IOException {
		return com.google.protobuf.GeneratedMessageV3
				.parseWithIOException(PARSER, input);
	}
	public static org.springframework.integration.transformer.proto.TestClass1 parseFrom(
			com.google.protobuf.CodedInputStream input,
			com.google.protobuf.ExtensionRegistryLite extensionRegistry)
			throws java.io.IOException {
		return com.google.protobuf.GeneratedMessageV3
				.parseWithIOException(PARSER, input, extensionRegistry);
	}

	@java.lang.Override
	public Builder newBuilderForType() { return newBuilder(); }
	public static Builder newBuilder() {
		return DEFAULT_INSTANCE.toBuilder();
	}
	public static Builder newBuilder(org.springframework.integration.transformer.proto.TestClass1 prototype) {
		return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
	}
	@java.lang.Override
	public Builder toBuilder() {
		return this == DEFAULT_INSTANCE
				? new Builder() : new Builder().mergeFrom(this);
	}

	@java.lang.Override
	protected Builder newBuilderForType(
			com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
		Builder builder = new Builder(parent);
		return builder;
	}
	/**
	 * Protobuf type {@code tutorial.TestClass1}
	 */
	public static final class Builder extends
			com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
			// @@protoc_insertion_point(builder_implements:tutorial.TestClass1)
			org.springframework.integration.transformer.proto.TestClass1OrBuilder {
		public static final com.google.protobuf.Descriptors.Descriptor
				getDescriptor() {
			return org.springframework.integration.transformer.proto.TestProtos.internal_static_tutorial_TestClass1_descriptor;
		}

		@java.lang.Override
		protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
				internalGetFieldAccessorTable() {
			return org.springframework.integration.transformer.proto.TestProtos.internal_static_tutorial_TestClass1_fieldAccessorTable
					.ensureFieldAccessorsInitialized(
							org.springframework.integration.transformer.proto.TestClass1.class, org.springframework.integration.transformer.proto.TestClass1.Builder.class);
		}

		// Construct using org.springframework.integration.transformer.proto.TestClass1.newBuilder()
		private Builder() {
			maybeForceBuilderInitialization();
		}

		private Builder(
				com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
			super(parent);
			maybeForceBuilderInitialization();
		}
		private void maybeForceBuilderInitialization() {
			if (com.google.protobuf.GeneratedMessageV3
							.alwaysUseFieldBuilders) {
			}
		}
		@java.lang.Override
		public Builder clear() {
			super.clear();
			bar_ = "";
			bitField0_ = (bitField0_ & ~0x00000001);
			qux_ = 0;
			bitField0_ = (bitField0_ & ~0x00000002);
			return this;
		}

		@java.lang.Override
		public com.google.protobuf.Descriptors.Descriptor
				getDescriptorForType() {
			return org.springframework.integration.transformer.proto.TestProtos.internal_static_tutorial_TestClass1_descriptor;
		}

		@java.lang.Override
		public org.springframework.integration.transformer.proto.TestClass1 getDefaultInstanceForType() {
			return org.springframework.integration.transformer.proto.TestClass1.getDefaultInstance();
		}

		@java.lang.Override
		public org.springframework.integration.transformer.proto.TestClass1 build() {
			org.springframework.integration.transformer.proto.TestClass1 result = buildPartial();
			if (!result.isInitialized()) {
				throw newUninitializedMessageException(result);
			}
			return result;
		}

		@java.lang.Override
		public org.springframework.integration.transformer.proto.TestClass1 buildPartial() {
			org.springframework.integration.transformer.proto.TestClass1 result = new org.springframework.integration.transformer.proto.TestClass1(this);
			int from_bitField0_ = bitField0_;
			int to_bitField0_ = 0;
			if (((from_bitField0_ & 0x00000001) != 0)) {
				to_bitField0_ |= 0x00000001;
			}
			result.bar_ = bar_;
			if (((from_bitField0_ & 0x00000002) != 0)) {
				result.qux_ = qux_;
				to_bitField0_ |= 0x00000002;
			}
			result.bitField0_ = to_bitField0_;
			onBuilt();
			return result;
		}

		@java.lang.Override
		public Builder clone() {
			return super.clone();
		}
		@java.lang.Override
		public Builder setField(
				com.google.protobuf.Descriptors.FieldDescriptor field,
				java.lang.Object value) {
			return super.setField(field, value);
		}
		@java.lang.Override
		public Builder clearField(
				com.google.protobuf.Descriptors.FieldDescriptor field) {
			return super.clearField(field);
		}
		@java.lang.Override
		public Builder clearOneof(
				com.google.protobuf.Descriptors.OneofDescriptor oneof) {
			return super.clearOneof(oneof);
		}
		@java.lang.Override
		public Builder setRepeatedField(
				com.google.protobuf.Descriptors.FieldDescriptor field,
				int index, java.lang.Object value) {
			return super.setRepeatedField(field, index, value);
		}
		@java.lang.Override
		public Builder addRepeatedField(
				com.google.protobuf.Descriptors.FieldDescriptor field,
				java.lang.Object value) {
			return super.addRepeatedField(field, value);
		}
		@java.lang.Override
		public Builder mergeFrom(com.google.protobuf.Message other) {
			if (other instanceof org.springframework.integration.transformer.proto.TestClass1) {
				return mergeFrom((org.springframework.integration.transformer.proto.TestClass1)other);
			} else {
				super.mergeFrom(other);
				return this;
			}
		}

		public Builder mergeFrom(org.springframework.integration.transformer.proto.TestClass1 other) {
			if (other == org.springframework.integration.transformer.proto.TestClass1.getDefaultInstance()) return this;
			if (other.hasBar()) {
				bitField0_ |= 0x00000001;
				bar_ = other.bar_;
				onChanged();
			}
			if (other.hasQux()) {
				setQux(other.getQux());
			}
			this.mergeUnknownFields(other.unknownFields);
			onChanged();
			return this;
		}

		@java.lang.Override
		public final boolean isInitialized() {
			return true;
		}

		@java.lang.Override
		public Builder mergeFrom(
				com.google.protobuf.CodedInputStream input,
				com.google.protobuf.ExtensionRegistryLite extensionRegistry)
				throws java.io.IOException {
			org.springframework.integration.transformer.proto.TestClass1 parsedMessage = null;
			try {
				parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
			} catch (com.google.protobuf.InvalidProtocolBufferException e) {
				parsedMessage = (org.springframework.integration.transformer.proto.TestClass1) e.getUnfinishedMessage();
				throw e.unwrapIOException();
			} finally {
				if (parsedMessage != null) {
					mergeFrom(parsedMessage);
				}
			}
			return this;
		}
		private int bitField0_;

		private java.lang.Object bar_ = "";
		/**
		 * <code>optional string bar = 1;</code>
		 * @return Whether the bar field is set.
		 */
		public boolean hasBar() {
			return ((bitField0_ & 0x00000001) != 0);
		}
		/**
		 * <code>optional string bar = 1;</code>
		 * @return The bar.
		 */
		public java.lang.String getBar() {
			java.lang.Object ref = bar_;
			if (!(ref instanceof java.lang.String)) {
				com.google.protobuf.ByteString bs =
						(com.google.protobuf.ByteString) ref;
				java.lang.String s = bs.toStringUtf8();
				if (bs.isValidUtf8()) {
					bar_ = s;
				}
				return s;
			} else {
				return (java.lang.String) ref;
			}
		}
		/**
		 * <code>optional string bar = 1;</code>
		 * @return The bytes for bar.
		 */
		public com.google.protobuf.ByteString
				getBarBytes() {
			java.lang.Object ref = bar_;
			if (ref instanceof String) {
				com.google.protobuf.ByteString b =
						com.google.protobuf.ByteString.copyFromUtf8(
								(java.lang.String) ref);
				bar_ = b;
				return b;
			} else {
				return (com.google.protobuf.ByteString) ref;
			}
		}
		/**
		 * <code>optional string bar = 1;</code>
		 * @param value The bar to set.
		 * @return This builder for chaining.
		 */
		public Builder setBar(
				java.lang.String value) {
			if (value == null) {
		throw new NullPointerException();
	}
	bitField0_ |= 0x00000001;
			bar_ = value;
			onChanged();
			return this;
		}
		/**
		 * <code>optional string bar = 1;</code>
		 * @return This builder for chaining.
		 */
		public Builder clearBar() {
			bitField0_ = (bitField0_ & ~0x00000001);
			bar_ = getDefaultInstance().getBar();
			onChanged();
			return this;
		}
		/**
		 * <code>optional string bar = 1;</code>
		 * @param value The bytes for bar to set.
		 * @return This builder for chaining.
		 */
		public Builder setBarBytes(
				com.google.protobuf.ByteString value) {
			if (value == null) {
		throw new NullPointerException();
	}
	bitField0_ |= 0x00000001;
			bar_ = value;
			onChanged();
			return this;
		}

		private int qux_ ;
		/**
		 * <code>optional int32 qux = 2;</code>
		 * @return Whether the qux field is set.
		 */
		public boolean hasQux() {
			return ((bitField0_ & 0x00000002) != 0);
		}
		/**
		 * <code>optional int32 qux = 2;</code>
		 * @return The qux.
		 */
		public int getQux() {
			return qux_;
		}
		/**
		 * <code>optional int32 qux = 2;</code>
		 * @param value The qux to set.
		 * @return This builder for chaining.
		 */
		public Builder setQux(int value) {
			bitField0_ |= 0x00000002;
			qux_ = value;
			onChanged();
			return this;
		}
		/**
		 * <code>optional int32 qux = 2;</code>
		 * @return This builder for chaining.
		 */
		public Builder clearQux() {
			bitField0_ = (bitField0_ & ~0x00000002);
			qux_ = 0;
			onChanged();
			return this;
		}
		@java.lang.Override
		public final Builder setUnknownFields(
				final com.google.protobuf.UnknownFieldSet unknownFields) {
			return super.setUnknownFields(unknownFields);
		}

		@java.lang.Override
		public final Builder mergeUnknownFields(
				final com.google.protobuf.UnknownFieldSet unknownFields) {
			return super.mergeUnknownFields(unknownFields);
		}


		// @@protoc_insertion_point(builder_scope:tutorial.TestClass1)
	}

	// @@protoc_insertion_point(class_scope:tutorial.TestClass1)
	private static final org.springframework.integration.transformer.proto.TestClass1 DEFAULT_INSTANCE;
	static {
		DEFAULT_INSTANCE = new org.springframework.integration.transformer.proto.TestClass1();
	}

	public static org.springframework.integration.transformer.proto.TestClass1 getDefaultInstance() {
		return DEFAULT_INSTANCE;
	}

	@java.lang.Deprecated public static final com.google.protobuf.Parser<TestClass1>
			PARSER = new com.google.protobuf.AbstractParser<TestClass1>() {
		@java.lang.Override
		public TestClass1 parsePartialFrom(
				com.google.protobuf.CodedInputStream input,
				com.google.protobuf.ExtensionRegistryLite extensionRegistry)
				throws com.google.protobuf.InvalidProtocolBufferException {
			return new TestClass1(input, extensionRegistry);
		}
	};

	public static com.google.protobuf.Parser<TestClass1> parser() {
		return PARSER;
	}

	@java.lang.Override
	public com.google.protobuf.Parser<TestClass1> getParserForType() {
		return PARSER;
	}

	@java.lang.Override
	public org.springframework.integration.transformer.proto.TestClass1 getDefaultInstanceForType() {
		return DEFAULT_INSTANCE;
	}

}

