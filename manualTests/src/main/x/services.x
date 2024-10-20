module TestServices  {
    @Inject Console console;

    void run() {
        console.print("*** service tests ***\n");

        console.print($"{tag()} creating service");
        TestService svc = new TestService();

//        TestService[] svcs = new TestService[4](_ -> new TestService());
//
//        console.print($|{tag()} calling service async/wait-style\
//                         | {svc.serviceName} {svc.serviceControl.statusIndicator}
//                         );
//        Int n = svc.calcSomethingBig(Duration.None);
//        console.print($"{tag()} async/wait-style result={n}");
//
//        Int n0 = svc.terminateExceptionally^("n0");
//        &n0.handle(e -> {
//            console.print($"{tag()} 4. expected exception={e.text}");
//            return -1;
//        });
//
//        try {
//            Int n1 = svc.terminateExceptionally("n1");
//            assert;
//        } catch (Exception e) {
//            console.print($"{tag()} 1. expected exception={e.text}");
//        }
//
//        Int n2 = svc.terminateExceptionally^("n2");
//        try {
//            n2++;
//            assert;
//        } catch (Exception e) {
//            console.print($"{tag()} 2. expected exception={e.text}");
//        }
//
//        assert &n2.assigned;
//        &n2.handle(e -> {
//            console.print($"{tag()} 3. expected exception={e.text}");
//            return -1;
//        });
//
//        @Inject Timer timer;
//        timer.start();
//
//        Exception[] unguarded = new Exception[];
//        using (new ecstasy.AsyncSection(unguarded.add)) {
//            Loop: for (TestService each : svcs) {
//                val i = Loop.count;
//                each.spin^(10_000).passTo(n -> {
//                    console.print($"{tag()} spin {i} yielded {n}; took {timer.elapsed.milliseconds} ms");
//                });
//            }
//        }
//        assert unguarded.empty;
//
//        // test timeout
//        import ecstasy.Timeout;
//        try {
//            using (Timeout timeout = new Timeout(Duration:0.5S, True)) {
//                svc.calcSomethingBig(Duration:30S);
//                assert;
//            }
//        } catch (TimedOut e) {}

        @Volatile Int responded = 0;
        Int           count     = 3;
        console.print($"\n\n\n *****************************************");
        for (Int i : 0 ..< count) {
            console.print($"calling service future-style: {i}");
            @Future Int result = svc.calcSomethingBig(Duration.ofSeconds(i));
//            &result.whenComplete((n, e) -> {
//                console.print($"{tag()} result={(n ?: e ?: "???")}");
//                // when the last result comes back - shut down
//                if (++responded == count) {
//                    svc.&done.set^(True);
//                }
//            });
         // console.print($"{result}");
         for (Int j : 0 ..< 1000) {} // this "fixes" the race
        }

        for (Int j : 0 ..< 10000) {}
        console.print($"*** wait for completion");
        Boolean done = svc.waitForCompletion();
        console.print($"done={done}; shutting down");

        // without the left side an exception would be reported by the default handler
        //Int ignoreException = svc.calcSomethingBig^(Duration:45S);
        //svc.serviceControl.shutdown();

//        try {
//            svc.spin(0);
//            assert;
//        } catch (Exception e) {
//            console.print($"expected: {e}");
//        }
    }

    service TestService {
        Now now = new Now();
        Int calcSomethingBig(Duration delay) {
            @Inject Console console;

            console.print($"\n*** prepare for: {delay}");
           now.delta(delay);

            console.print($"\n********* calculating for: {delay}");
            return 0;
//            @Inject Timer timer;
//            @Future Int   result;
//            timer.schedule(delay, () -> {
//              //  console.print($"{tag()} triggered {delay.seconds}");
//              //  console.print($"{tag()} setting result {delay.seconds}");
//                result=delay.seconds;
//            });
//
//          //  console.print($"{tag()} preparing to return result {delay}");
//            return result;
        }

        Int spin(Int iters) {
            Int sum = 0;
            for (Int i : iters..1) {
                sum += i;
            }

            return sum;
        }

        Int terminateExceptionally(String message) {
            throw new Exception(message);
        }

        @Future Boolean done;

        Boolean waitForCompletion() {
            return done;
        }

    }

    static String tag() {
//        static Now now = new Now();
//        Int sec = now.delta().seconds; // removing this "fixes" the race; this "is" the race?
        return /*$"{sec}:\t" +*/ (this:service.serviceName == "TestService" ? "[svc ]" : "[main]");
    }

    service Now {
        @Inject Clock clock;
        Time base = clock.now;
        Duration zero = base - base;

        public Time now() {
            return clock.now;
        }

        public Duration delta(Duration delay) {
            console.print($"***** delta {delay}");
            return zero;//now() - base;
        }
    }
}