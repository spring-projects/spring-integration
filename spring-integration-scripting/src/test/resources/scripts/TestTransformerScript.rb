class Transformer
  def transform(payload, arg)
    "ruby-#{payload}-#{arg}"
  end
end

Transformer.new.transform(payload, foo)
