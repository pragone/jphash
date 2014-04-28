# About jpHash
This is library based on the great work behind pHash, the perceptual hash library (http://www.phash.org/).

My main problem with using this library is that it is written in C++ and I'm a Java guy. So I decided to have a go at converting it to Java.

Currently, only the Radial Image hash algorithm is implemented as that's the only one I've needed yet, but implementing the rest of the image hashing algorithms should be trivial as they are all based on the same preprocessing of the image (grayscaling, correcting brightness, blurring and scaling)... and all that is done already.

# Efficiency
I've invested quite a bit in making this efficient. Some personal benchmarks have given me execution times comparable to the C++ version. Of course that meant that I had to implement my own grayscaling, resizing and bluring algorithms.

# Licence
I still need to add all the licencing info, but the idea is that it be LGPL so it can be included in other distributed work. I'm still checking, but I believe this is compatible with the GPL version that pHash is on.

# Usage
Simple:
```
RadialImageHash hash1 = jpHash.getImageRadialHash("/path/to/image");
System.out.println("Hash1: " + hash1);
RadialImageHash hash2 = jpHash.getImageRadialHash("/path/to/other/image");
System.out.println("Hash2: " + hash2);

System.out.println("Similarity: " + jpHash.getSimilarity(hash1, hash2));

```

You can also persist the string representation of the hash and recover it with: RadialHash.fromString(String)

# Links
Some links of interest that this work is based upon:

* http://phash.org/docs/pubs/thesis_zauner.pdf -> Available from http://phash.org/docs/
* http://perso.uclouvain.be/fstandae/PUBLIS/26.pdf -> PRACTICAL EVALUATION OF A RADIAL SOFT HASH ALGORITHM
* http://www.eurasip.org/Proceedings/Eusipco/2002/articles/paper745.pdf -> RASH:RAdon Soft Hash algorithm
* ftp://bjhd.org/papers/PR/ICIP/ICIP05/defevent/papers/cr2347.pdf -> ROBUST IMAGE HASHING BASED ON RADIAL VARIANCE OF PIXELS

For info on building an efficient index for Nearest neighbour search (my planned next stage). These are some of the papers I've found:
* http://arxiv.org/pdf/1202.6101.pdf -> Maximum Inner-Product Search using Tree Data-structures
* Maximum Inner-Product Search using Tree Data-structures
* http://www.vldb.org/journal/VLDBJ3/P517.pdf -> The W-Tree: An Index Structure for High-Dimensional Data
* http://users.dcc.uchile.cl/~bebustos/cursos/2009/cc68p/papers/BKK96%20The%20X-tree%20an%20index%20structure%20for%20high-dimensional%20data.pdf -> The X-tree: An Index Structure for High-Dimensional Data

