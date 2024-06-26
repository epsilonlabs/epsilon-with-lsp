rule T2T 
	match l : Left!Tree in: Left!Tree.all.select(t|t.name = "t1")
	with r : Right!Tree in: Right!Tree.all.select(t|t.name = "t1"){
	
	compare : l.name = r.name and 
		((l.parent == null and r.parent == null) or l.parent.matches(r.parent))
}

post {
	for (t in matchTrace.reduced) {
		(t.left + " <-> " + t.right).println();
	}
}