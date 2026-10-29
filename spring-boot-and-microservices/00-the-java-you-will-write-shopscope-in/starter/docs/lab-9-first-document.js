// Demo 0, Lab 9: a first document in MongoDB. Start MongoDB and open a shell:
//   docker run --rm -d --name shopscope-mongo -p 27017:27017 mongo:8
//   docker exec -it shopscope-mongo mongosh shopscope
// then paste the lines below one at a time.

// A customer is a document: the address book lives inside it, not in a second table.
db.customers.insertOne({
  _id: "c-1",
  email: "priya@example.com",
  name: "Priya",
  customerGroup: "retail",
  addresses: [
    { label: "home", line1: "12 MG Road", city: "Bengaluru", country: "IN", default: true },
    { label: "office", line1: "4 Residency Road", city: "Bengaluru", country: "IN", default: false }
  ]
});

// Read it back. No join: the addresses come with it.
db.customers.findOne({ _id: "c-1" });

// Query inside the array.
db.customers.find({ "addresses.city": "Bengaluru" }, { name: 1, "addresses.label": 1 });

// Change one address in place.
db.customers.updateOne({ _id: "c-1", "addresses.label": "office" }, { $set: { "addresses.$.line1": "5 Residency Road" } });

// There is no schema yet: this document has a field the first one does not. Demo 13 discusses when that is a feature.
db.customers.insertOne({ _id: "c-2", email: "arjun@example.com", name: "Arjun", customerGroup: "wholesale", gstNumber: "29ABCDE1234F1Z5", addresses: [] });
db.customers.countDocuments();
