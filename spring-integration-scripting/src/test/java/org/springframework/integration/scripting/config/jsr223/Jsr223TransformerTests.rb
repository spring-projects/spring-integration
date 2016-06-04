class Transformer
  def transform(payload)
    "ruby-#{payload}"
  end
end

Transformer.new.transform(payload)
