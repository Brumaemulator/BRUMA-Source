// SPDX-License-Identifier: MIT
// BRUMA test-only entry point; not part of the production runtime.
#include <ruby.h>
void Init_ext(void);
int main(int argc, char **argv) {
 ruby_sysinit(&argc,&argv);
 RUBY_INIT_STACK;
 ruby_init();
 Init_ext();
 return ruby_run_node(ruby_options(argc,argv));
}
