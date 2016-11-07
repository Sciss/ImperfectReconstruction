```scala
implicit class NextPrime(n: Int) {
  def nextPrime: Int = 
    Iterator.from(n).filter(_.isPrime).next
    
  def isPrime: Boolean = n > 1 && (2 until n).forall(m => n % m != 0)
}
```

# Group 1

    (7*7).nextPrime.sqrt == 7.28011

    --input /media/hhrutz/AC6E5D6F6E5D3376/projects/Imperfect/scans/notebook2016/universe-out/group-1/frame-%d.png --output /media/hhrutz/AC6E5D6F6E5D3376/projects/Imperfect/scans/notebook2016/universe-rsmp/group-1/frame-rsmp-%d.png --factor 7.28011 -g 0.818 --kaiser-beta 7.7,8.8,7.7 --roll-off 0.85,0.83,0.87 --zero-crossings 13,13,12 --noise 0.045

# Group 2

    (4.2*60*24 / 1225).squared.toInt.nextPrime.sqrt == 5.3851647

    --input /media/hhrutz/AC6E5D6F6E5D3376/projects/Imperfect/scans/notebook2016/universe-out/group-2/frame-%d.png --output /media/hhrutz/AC6E5D6F6E5D3376/projects/Imperfect/scans/notebook2016/universe-rsmp/group-2/frame-rsmp-%d.png --factor 5.3851647 -g 0.815 --kaiser-beta 6.5,8.5,7.5 --roll-off 0.83,0.85,0.87 --zero-crossings 13,11,12 --noise 0.045

# Group 3

    (4.2*60*24 / 1153).squared.toInt.nextPrime.sqrt == 5.3851647

    --input /media/hhrutz/AC6E5D6F6E5D3376/projects/Imperfect/scans/notebook2016/universe-out/group-3/frame-%d.png --output /media/hhrutz/AC6E5D6F6E5D3376/projects/Imperfect/scans/notebook2016/universe-rsmp/group-3/frame-rsmp-%d.png --factor 5.3851647 -g 0.817 --kaiser-beta 6.66,8.1,7.77 --roll-off 0.80,0.90,0.85 --zero-crossings 11,10,11 --noise 0.045

# Group 4

    (4.2*60*24 / 1041).squared.toInt.nextPrime.sqrt == 6.0827627

    --input /media/hhrutz/AC6E5D6F6E5D3376/projects/Imperfect/scans/notebook2016/universe-out/group-4/frame-%d.png --output /media/hhrutz/AC6E5D6F6E5D3376/projects/Imperfect/scans/notebook2016/universe-rsmp/group-4/frame-rsmp-%d.png --factor 6.0827627 -g 0.79 --kaiser-beta 7.66,7.1,8.77 --roll-off 0.83,0.94,0.85 --zero-crossings 10,12,10 --noise 0.046

# Group 5

    (7*7).nextPrime.sqrt == 7.28011

    --input /media/hhrutz/AC6E5D6F6E5D3376/projects/Imperfect/scans/notebook2016/universe-out/group-5/frame-%d.png --output /media/hhrutz/AC6E5D6F6E5D3376/projects/Imperfect/scans/notebook2016/universe-rsmp/group-5/frame-rsmp-%d.png --factor 7.28011

# Group 6

    (4.2*60*24 / 1067).squared.toInt.nextPrime.sqrt = 6.0827627

    --input /media/hhrutz/AC6E5D6F6E5D3376/projects/Imperfect/scans/notebook2016/universe-out/group-6/frame-%d.png --output /media/hhrutz/AC6E5D6F6E5D3376/projects/Imperfect/scans/notebook2016/universe-rsmp/group-6/frame-rsmp-%d.png --factor 6.0827627 -g 0.785 --kaiser-beta 7.66,6.5,8.07 --roll-off 0.93,0.90,0.85 --zero-crossings 9,12,13 --noise 0.0465

# Group 7

    (4.2*60*24 / 979).squared.toInt.nextPrime.sqrt == 6.4031243

    --input /media/hhrutz/AC6E5D6F6E5D3376/projects/Imperfect/scans/notebook2016/universe-out/group-7/frame-%d.png --output /media/hhrutz/AC6E5D6F6E5D3376/projects/Imperfect/scans/notebook2016/universe-rsmp/group-7/frame-rsmp-%d.png --factor 6.4031243 -g 0.81 --kaiser-beta 8.5,7.5,6.5 --roll-off 0.86,0.85,0.84 --zero-crossings 12,13,14 --noise 0.044
    
 # Group 8
 
     (4.2*60*24 / 1191).squared.toInt.nextPrime.+(1).nextPrime.+(1).nextPrime.sqrt
     == 6.0827627
 
     --input /media/hhrutz/AC6E5D6F6E5D3376/projects/Imperfect/scans/notebook2016/universe-out/group-8/frame-%d.png --output /media/hhrutz/AC6E5D6F6E5D3376/projects/Imperfect/scans/notebook2016/universe-rsmp/group-8/frame-rsmp-%d.png --factor 6.0827627 -g 0.77 --kaiser-beta 6.5,7.5,6.5 --roll-off 0.81,0.85,0.84 --zero-crossings 12,9,14 --noise 0.045
