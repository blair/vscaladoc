try {

root = new File(basedir, 'target/site/scaladocs')
assert root.exists()
assert root.isDirectory()
assert new File(root, 'overview.html').exists()
//assert new File(root, 'index.html').exists()
//assert new File(root, 'all-classes.html').exists()
//assert new File(root, 'all-classes.js').exists()
//assert new File(root, 'all-classes.css').exists()
//assert new File(root, 'content.js').exists()
//assert new File(root, 'content.css').exists()
//assert new File(root, 'jquery-1.3.2.js').exists()
//assert new File(root, '_images/class.png').exists()
//assert new File(root, '_images/object.png').exists()
//assert new File(root, '_images/trait.png').exists()

return true

} catch (Throwable t) {
  t.printStackTrace()
  return false
}