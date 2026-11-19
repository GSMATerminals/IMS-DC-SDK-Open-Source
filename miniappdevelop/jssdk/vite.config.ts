// vite.config.ts
import { defineConfig } from 'vite';
import path from 'path';
import terser from '@rollup/plugin-terser';

export default defineConfig({
    build: {
        lib: {
            entry: path.resolve(__dirname, 'src/index.ts'),
            name: 'jssdk',
            fileName: () => 'jssdk.js',
            formats: ['es']
        },
        rollupOptions: {
            external: [],
            output: {
                exports: 'named',
                plugins: [
                    terser({
                        compress: {
                            drop_console: false,
                            drop_debugger: true,
                            // ★ 保留未使用的函数
                            unused: false,
                            // ★ 保留副作用代码
                            side_effects: false,
                            passes: 2,
                            unsafe: true,
                        },
                        mangle: {
                            toplevel: true,
                        },
                        format: {
                            comments: false,
                            beautify: false,
                        },
                        module: true,
                    })
                ]
            }
        },
        outDir: 'dist',
        emptyOutDir: true,
        minify: false,
        sourcemap: false,
        target: 'es2015',
    }
});